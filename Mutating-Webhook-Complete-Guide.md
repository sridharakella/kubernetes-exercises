# Kubernetes Mutating Webhook — Complete Code Walkthrough & DigitalOcean Deployment Guide

## Repository: `Manohar-1305/mutating-webhook`

---

## What This Project Does

This is a **Python-based Mutating Admission Webhook** that automatically **copies labels from a Kubernetes Namespace into every Pod** created in that namespace. This solves a real production problem: Kubernetes does NOT propagate namespace labels to Pods automatically.

**Real-world use case:** At scale (thousands of namespaces, hundreds of thousands of deployments), a dedicated team labels namespaces with `team=testing`, `env=prod`, etc. The webhook ensures every Pod automatically inherits those labels — no developer action needed. This drives scheduling, cost tracking, security policies, and monitoring.

---

## Repository File Structure

```
mutating-webhook/
├── webhook.py                  ← The webhook server (Python/Flask) — THE BRAIN
├── Dockerfile                  ← Builds the webhook into a container image
├── requirements.txt            ← Python dependencies (flask, kubernetes)
├── san.cnf                     ← TLS certificate SAN configuration
├── steps.sh                    ← Shell script with all setup commands
├── rbac.yaml                   ← ServiceAccount + ClusterRole + ClusterRoleBinding
├── webhook-deployment.yaml     ← Deployment + Service for the webhook
├── webhook-configuration.yaml  ← MutatingWebhookConfiguration (registers the webhook)
├── testing-pod.yaml            ← Test Pod for scheduling validation
└── README.md                   ← Documentation
```

---

## How the Entire System Works (End-to-End Flow)

```
Developer runs:  kubectl create deployment nginx --image=nginx -n testing
                        │
                        ▼
              ┌──────────────────┐
              │   API Server      │  1. Receives the Pod CREATE request
              │                   │  2. Checks: "Are there any MutatingWebhookConfigurations?"
              └────────┬─────────┘  3. Finds: ns-label-webhook matches (Pod CREATE + namespace
                       │               has label ns-label-sync=enabled)
                       │
                       │  4. Sends AdmissionReview (HTTPS POST) to:
                       │     https://webhook-service.webhook-system.svc:443/mutate
                       ▼
              ┌──────────────────┐
              │  Webhook Pod      │  5. Flask receives the AdmissionReview JSON
              │  (webhook.py)     │  6. Extracts: which namespace? what kind of object?
              │                   │  7. Calls Kubernetes API: "Give me labels for namespace 'testing'"
              │                   │  8. Compares: namespace labels vs pod labels
              │                   │  9. Builds JSON Patch: add missing labels
              └────────┬─────────┘ 10. Returns AdmissionReview response with patch
                       │
                       ▼
              ┌──────────────────┐
              │   API Server      │ 11. Applies the JSON Patch to the Pod spec
              │                   │ 12. Stores the MUTATED Pod in etcd
              └──────────────────┘ 13. Pod now has namespace labels automatically!
```

---

## FILE-BY-FILE CODE EXPLANATION

---

### 1. `webhook.py` — The Webhook Server (Core Logic)

This is the heart of the project. It's a Flask HTTPS server that listens on `/mutate`.

```python
import json
import base64
from flask import Flask, request, jsonify
from kubernetes import client, config
```

**What these imports do:**
- `json` / `base64` — needed to build and encode the JSON Patch response
- `Flask` — lightweight Python web framework that handles the HTTPS server
- `kubernetes` client — official Python SDK to talk to the Kubernetes API (to read namespace labels)

```python
app = Flask(__name__)
config.load_incluster_config()
```

**`config.load_incluster_config()`** — This is critical. When the webhook runs inside a Pod, it automatically discovers the API server address and authentication token from:
- `/var/run/secrets/kubernetes.io/serviceaccount/token`
- `/var/run/secrets/kubernetes.io/serviceaccount/ca.crt`

This is why RBAC (ServiceAccount) is mandatory — without it, this call would authenticate but have no permissions.

```python
def esc(k):
    return k.replace("~", "~0").replace("/", "~1")
```

**JSON Patch escape function.** JSON Patch paths use `/` as a separator (e.g., `/metadata/labels/app`). If a label key contains `/` (like `kubernetes.io/metadata.name`), it would break the path. RFC 6901 requires escaping: `~` becomes `~0`, `/` becomes `~1`. So `kubernetes.io/metadata.name` becomes `kubernetes.io~1metadata.name`.

```python
@app.route("/mutate", methods=["POST"])
def mutate():
    review = request.get_json(silent=True)
    if not review or "request" not in review:
        return allow()
```

**The `/mutate` endpoint.** The API server sends a POST request with an `AdmissionReview` JSON body. If the body is malformed or missing, we just allow the request to proceed (fail-open).

```python
    req = review["request"]
    uid = req.get("uid")
    namespace = req.get("namespace")
    kind = req.get("kind", {}).get("kind")
    pod = req.get("object", {})
```

**Extract key information from the AdmissionReview:**
- `uid` — unique identifier for this admission request (must be echoed back in the response)
- `namespace` — which namespace the Pod is being created in
- `kind` — what type of object (should be "Pod")
- `object` — the actual Pod spec being created

```python
    if kind != "Pod" or not namespace:
        return allow(uid)
```

**Safety guard:** Only mutate Pods. If something else comes in (shouldn't happen based on the webhook config rules, but defensive coding), let it through unchanged.

```python
    pod_labels = pod.get("metadata", {}).get("labels", {}) or {}
```

**Get existing Pod labels.** The `or {}` handles the case where labels is `None` (not just missing).

```python
    v1 = client.CoreV1Api()
    ns_labels = v1.read_namespace(namespace).metadata.labels or {}
```

**This is where the magic happens.** The webhook calls the Kubernetes API to read the namespace object and get its labels. This is why RBAC is needed — without the `get namespaces` permission, this line fails with `403 Forbidden`.

```python
    patch = []
    for k, v in ns_labels.items():
        if k in pod_labels:
            continue
        patch.append({
            "op": "add",
            "path": "/metadata/labels/" + esc(k),
            "value": v
        })
```

**Build the JSON Patch.** For each namespace label:
- If the Pod ALREADY has this label → skip it (idempotent — never overwrite developer labels)
- If the Pod DOESN'T have it → add it via a JSON Patch "add" operation

Example patch for namespace with `env=prod` and `team=testing`:
```json
[
  {"op": "add", "path": "/metadata/labels/env", "value": "prod"},
  {"op": "add", "path": "/metadata/labels/team", "value": "testing"}
]
```

```python
    if not patch:
        return allow(uid)
```

**No changes needed?** Just allow the request as-is (no patch).

```python
    return jsonify({
        "apiVersion": "admission.k8s.io/v1",
        "kind": "AdmissionReview",
        "response": {
            "uid": uid,
            "allowed": True,
            "patchType": "JSONPatch",
            "patch": base64.b64encode(json.dumps(patch).encode()).decode()
        }
    })
```

**Return the mutated response.** The API server expects:
- `uid` — must match the request (identifies which request this responds to)
- `allowed: True` — yes, allow this Pod creation
- `patchType: JSONPatch` — we're using RFC 6902 JSON Patch format
- `patch` — the base64-encoded JSON Patch array

The base64 encoding is a Kubernetes requirement — patches must be base64-encoded in the response.

```python
def allow(uid=None):
    return jsonify({
        "apiVersion": "admission.k8s.io/v1",
        "kind": "AdmissionReview",
        "response": {
            "uid": uid,
            "allowed": True
        }
    })
```

**Helper function:** Returns a "just allow it, no changes" response.

```python
if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=8443,
        ssl_context=("/tls/tls.crt", "/tls/tls.key")
    )
```

**Start the HTTPS server.** Listens on all interfaces (`0.0.0.0`), port `8443`, using the TLS certificate and key mounted from the Kubernetes Secret. This is HTTPS — Kubernetes refuses to talk to webhooks over plain HTTP.

---

### 2. `Dockerfile` — Container Build

```dockerfile
FROM python:3.11-slim
WORKDIR /app
RUN pip install flask kubernetes
COPY app.py /app/app.py
EXPOSE 8443
CMD ["python", "/app/app.py"]
```

**Line by line:**
- `FROM python:3.11-slim` — minimal Python 3.11 base image (~150MB vs ~900MB for full)
- `WORKDIR /app` — sets the working directory inside the container
- `RUN pip install flask kubernetes` — installs Flask (web server) and the official Kubernetes Python client
- `COPY app.py /app/app.py` — copies the webhook code into the container

**NOTE:** The Dockerfile references `app.py` but the repo file is named `webhook.py`. You'll need to either rename the file or update the Dockerfile (covered in deployment steps below).

- `EXPOSE 8443` — documents that the container listens on port 8443
- `CMD` — starts the Flask HTTPS server when the container runs

---

### 3. `san.cnf` — TLS Certificate Configuration

```ini
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
CN = webhook-service.webhook-system.svc

[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = webhook-service
DNS.2 = webhook-service.webhook-system
DNS.3 = webhook-service.webhook-system.svc
```

**Why SAN certificates are mandatory:**

Kubernetes (and Go's TLS stack) completely ignores the `CN` (Common Name) field. It only validates **Subject Alternative Names (SANs)**. Without SANs, you get:
```
x509: certificate relies on legacy Common Name field, use SANs instead
```

The three DNS entries cover every way Kubernetes might address the service:
- `webhook-service` — short name (within the same namespace)
- `webhook-service.webhook-system` — namespaced name
- `webhook-service.webhook-system.svc` — fully qualified (what the API server actually uses)

---

### 4. `rbac.yaml` — RBAC Permissions

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: webhook-sa
  namespace: webhook-system
```

**ServiceAccount** — gives the webhook Pod an identity. Without this, it runs as the `default` ServiceAccount which has zero permissions.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: webhook-ns-reader
rules:
- apiGroups: [""]
  resources: ["namespaces"]
  verbs: ["get"]
```

**ClusterRole** — defines the permission: "you can GET namespace objects." It's a ClusterRole (not Role) because namespaces are cluster-scoped resources. The permission is minimal — only `get`, not `list`, `watch`, `update`, or `delete`.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: webhook-ns-reader-binding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: webhook-ns-reader
subjects:
- kind: ServiceAccount
  name: webhook-sa
  namespace: webhook-system
```

**ClusterRoleBinding** — connects the ServiceAccount to the ClusterRole. Without this binding, the role and the account exist but aren't linked — the Pod still gets `403 Forbidden`.

---

### 5. `webhook-deployment.yaml` — Deployment + Service

**Deployment** runs the webhook Pod:
- `serviceAccountName: webhook-sa` — uses the RBAC-enabled account
- `image: manoharshetty507/webhook:v1` — the container image
- `containerPort: 8443` — matches the Flask server port
- TLS secret (`webhook-tls`) is mounted at `/tls` — Flask reads certs from there

**Service** makes the webhook reachable:
- `port: 443` → `targetPort: 8443` — API server connects on standard HTTPS port 443, traffic is forwarded to the Flask app on 8443
- `selector: app: webhook` — routes traffic to the webhook Pod

---

### 6. `webhook-configuration.yaml` — MutatingWebhookConfiguration

This is what **registers** the webhook with the Kubernetes API server:

- **`clientConfig.service`** — tells the API server where to send admission requests: `webhook-service` in `webhook-system` namespace, path `/mutate`, port `443`
- **`caBundle`** — base64-encoded CA certificate that the API server uses to verify the webhook's TLS cert
- **`rules`** — only trigger on `CREATE` operations for `pods` in API group `""` (core) version `v1`
- **`failurePolicy: Ignore`** — if the webhook is unreachable, allow the Pod anyway (fail-open, safe for production)
- **`timeoutSeconds: 5`** — if the webhook doesn't respond in 5 seconds, skip it
- **`namespaceSelector`** — only call the webhook for namespaces that have the label `ns-label-sync=enabled` (opt-in model)

---

### 7. `testing-pod.yaml` — Test Pod with Node Affinity

Used to test scheduling — a Pod with `nodeSelector` that requires specific labels on a node. It stays `Pending` until a node has matching labels, proving the webhook + scheduling pipeline works.

---

## DEPLOYING ON DIGITALOCEAN — STEP BY STEP

**Important DigitalOcean difference:** DigitalOcean Kubernetes (DOKS) is a **managed** cluster. You do NOT have access to `/etc/kubernetes/manifests/kube-apiserver.yaml`. The good news: DOKS already has `MutatingAdmissionWebhook` enabled by default. So you can skip Step 1 from the repo's README.

---

### Prerequisites

On your local machine (Mac/Linux), install:
- `doctl` — DigitalOcean CLI
- `kubectl` — Kubernetes CLI
- `docker` — Docker for building images
- `openssl` — for generating TLS certs (usually pre-installed)
- `git` — to clone the repo

---

### Step 1: Authenticate with DigitalOcean

```bash
# Install doctl
brew install doctl          # macOS
# OR
sudo snap install doctl     # Linux

# Generate API token at: https://cloud.digitalocean.com/account/api/tokens
doctl auth init
# Paste your API token when prompted
```

**Expected result:** `Validating token... ✔`

---

### Step 2: Create a Kubernetes Cluster

```bash
doctl kubernetes cluster create webhook-cluster \
  --region nyc1 \
  --version latest \
  --size s-2vcpu-4gb \
  --count 2
```

**What happens:** DigitalOcean provisions a managed K8s cluster with 2 nodes (~5-8 minutes). `doctl` automatically configures `~/.kube/config`.

**Expected result:**
```
Notice: Cluster created, fetching credentials
Notice: Adding cluster credentials to kubeconfig
Notice: Setting current-context to do-nyc1-webhook-cluster
```

**Verify:**
```bash
kubectl get nodes
```
Both nodes should show `Ready`.

---

### Step 3: Create a Container Registry

```bash
# Create private registry
doctl registry create webhook-registry --subscription-tier starter

# Log Docker into it
doctl registry login

# Connect registry to cluster (so nodes can pull images)
doctl kubernetes cluster registry add webhook-cluster
```

---

### Step 4: Clone the Repository

```bash
git clone https://github.com/Manohar-1305/mutating-webhook.git
cd mutating-webhook
```

---

### Step 5: Fix the Dockerfile

The repo's Dockerfile references `app.py` but the actual file is `webhook.py`. Fix it:

```bash
# Option A: Rename the file
cp webhook.py app.py

# Option B: OR fix the Dockerfile
sed -i 's/app.py/webhook.py/g' Dockerfile
# And also update the CMD line if needed
```

---

### Step 6: Build and Push the Docker Image

```bash
# Build
docker build -t registry.digitalocean.com/webhook-registry/webhook:v1 .

# Push
docker push registry.digitalocean.com/webhook-registry/webhook:v1
```

**Expected result:** Image pushed successfully to DOCR.

---

### Step 7: Create the `webhook-system` Namespace

```bash
kubectl create namespace webhook-system
```

**Expected result:** `namespace/webhook-system created`

---

### Step 8: Generate TLS Certificates

```bash
# Generate private key
openssl genrsa -out tls.key 2048

# Generate CSR using the SAN config from the repo
openssl req -new -key tls.key -out tls.csr -config san.cnf

# Self-sign the certificate
openssl x509 -req -in tls.csr -signkey tls.key \
  -out tls.crt -days 365 \
  -extensions v3_req -extfile san.cnf
```

**Verify SAN is present:**
```bash
openssl x509 -in tls.crt -noout -text | grep -A3 "Subject Alternative Name"
```

**Expected output:**
```
X509v3 Subject Alternative Name:
    DNS:webhook-service, DNS:webhook-service.webhook-system,
    DNS:webhook-service.webhook-system.svc
```

**Create the Kubernetes Secret:**
```bash
kubectl create secret tls webhook-tls \
  --cert=tls.crt \
  --key=tls.key \
  -n webhook-system
```

**Expected result:** `secret/webhook-tls created`

---

### Step 9: Apply RBAC

```bash
kubectl apply -f rbac.yaml
```

**Expected result:**
```
serviceaccount/webhook-sa created
clusterrole.rbac.authorization.k8s.io/webhook-ns-reader created
clusterrolebinding.rbac.authorization.k8s.io/webhook-ns-reader-binding created
```

---

### Step 10: Update and Apply the Deployment

Edit `webhook-deployment.yaml` to use YOUR image:

```bash
sed -i 's|manoharshetty507/webhook:v1|registry.digitalocean.com/webhook-registry/webhook:v1|g' webhook-deployment.yaml
```

Apply:
```bash
kubectl apply -f webhook-deployment.yaml
```

**Verify Pod is running:**
```bash
kubectl get pods -n webhook-system
```

**Expected result:**
```
NAME                                READY   STATUS    RESTARTS   AGE
ns-label-webhook-7bbb6dc87b-x2k4n  1/1     Running   0          30s
```

**If Pod is NOT Running, debug:**
```bash
kubectl describe pod -n webhook-system -l app=webhook
kubectl logs -n webhook-system -l app=webhook
```

---

### Step 11: Apply the MutatingWebhookConfiguration

First, get the base64-encoded CA bundle:

```bash
CA_BUNDLE=$(base64 -w0 tls.crt)
# On macOS: CA_BUNDLE=$(base64 tls.crt | tr -d '\n')
```

Update the webhook configuration with YOUR CA bundle:

```bash
sed -i "s|caBundle:.*|caBundle: ${CA_BUNDLE}|g" webhook-configuration.yaml
```

Apply:
```bash
kubectl apply -f webhook-configuration.yaml
```

**Verify:**
```bash
kubectl get mutatingwebhookconfiguration
```

**Expected result:**
```
NAME               WEBHOOKS   AGE
ns-label-webhook   1          10s
```

---

### Step 12: TEST — See the Webhook in Action

**Create a test namespace with labels:**

```bash
kubectl create namespace testing
kubectl label namespace testing \
  ns-label-sync=enabled \
  env=testing \
  team=qa \
  --overwrite
```

**Verify namespace labels:**
```bash
kubectl get namespace testing --show-labels
```

**Expected result:**
```
NAME      STATUS   AGE   LABELS
testing   Active   5s    env=testing,kubernetes.io/metadata.name=testing,
                         ns-label-sync=enabled,team=qa
```

**Create a deployment:**
```bash
kubectl create deployment nginx --image=nginx -n testing
```

**Check Pod labels (THE MAGIC MOMENT):**
```bash
kubectl get pods -n testing --show-labels
```

**Expected result:**
```
NAME                     READY   STATUS    RESTARTS   AGE   LABELS
nginx-7854ff8877-jhpw6   1/1     Running   0          5s    app=nginx,env=testing,
    kubernetes.io/metadata.name=testing,ns-label-sync=enabled,
    pod-template-hash=7854ff8877,team=qa
```

**The labels `env=testing`, `team=qa`, and `ns-label-sync=enabled` were automatically injected by the webhook!** The developer's manifest only had `app=nginx` — everything else came from the namespace.

**Verify with JSON output:**
```bash
kubectl get pod -n testing -o jsonpath='{.items[0].metadata.labels}' | python3 -m json.tool
```

---

### Step 13: Test Scheduling (Optional Advanced Test)

This proves labels can drive pod scheduling:

```bash
# Apply the testing pod (has nodeSelector requiring team=testing)
kubectl apply -f testing-pod.yaml
```

**Expected:** Pod stays `Pending` — no node has `team=testing` label.

```bash
kubectl get pods -n testing
# STATUS: Pending
```

**Label a node:**
```bash
# Get node name
NODE=$(kubectl get nodes -o jsonpath='{.items[0].metadata.name}')

# Label it
kubectl label node $NODE team=testing
```

**Expected:** Pod immediately schedules and becomes `Running`.

```bash
kubectl get pods -n testing
# STATUS: Running
```

---

### Step 14: Test Namespace Without the Opt-In Label

```bash
kubectl create namespace no-webhook
kubectl create deployment nginx --image=nginx -n no-webhook
kubectl get pods -n no-webhook --show-labels
```

**Expected result:** Pod only has default labels (`app=nginx`, `pod-template-hash=xxx`). **No namespace labels injected** — because the namespace doesn't have `ns-label-sync=enabled`.

This proves the `namespaceSelector` in the webhook configuration works correctly.

---

## Cleanup

```bash
# Delete test resources
kubectl delete namespace testing
kubectl delete namespace no-webhook

# Delete webhook resources
kubectl delete mutatingwebhookconfiguration ns-label-webhook
kubectl delete -f webhook-deployment.yaml
kubectl delete -f rbac.yaml
kubectl delete secret webhook-tls -n webhook-system
kubectl delete namespace webhook-system

# Delete the cluster (stops billing)
doctl kubernetes cluster delete webhook-cluster --force

# Delete the registry (optional)
doctl registry delete webhook-registry --force
```

---

## Common Failure Points & Debugging

| Symptom | Cause | Fix |
|---|---|---|
| Webhook never called, no logs | `MutatingAdmissionWebhook` not enabled on API server | On DOKS it's enabled by default. On kubeadm/k3s, edit the API server manifest |
| `x509: certificate signed by unknown authority` | `caBundle` in webhook-configuration.yaml doesn't match the cert | Regenerate: `base64 -w0 tls.crt` and update the yaml |
| `x509: certificate relies on legacy Common Name` | Certificate has no SANs | Regenerate using `san.cnf` with `subjectAltName` |
| `403 Forbidden` in webhook logs | Missing RBAC | Apply `rbac.yaml` and ensure `serviceAccountName: webhook-sa` is in the Deployment |
| Pods stuck in `Pending` forever | Webhook is crashing, `failurePolicy: Fail` blocks everything | Check webhook logs, use `failurePolicy: Ignore` |
| Service has no endpoints | Service selector doesn't match Pod labels | Both must have `app: webhook` |
| Image pull error | Registry not connected to cluster | Run `doctl kubernetes cluster registry add webhook-cluster` |

---

## Cost Estimate (DigitalOcean)

| Resource | Monthly Cost |
|---|---|
| DOKS cluster (2 nodes, s-2vcpu-4gb) | ~$48/month |
| Container Registry (Starter) | Free (500 MB) |
| **Total** | **~$48/month** |

Billing is hourly — delete immediately after testing to minimize costs.
