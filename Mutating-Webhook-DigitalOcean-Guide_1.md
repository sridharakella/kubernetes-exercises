# Kubernetes Mutating Webhook on DigitalOcean — Complete Step-by-Step Guide

## Repository Validation: `Manohar-1305/mutating-webhook`

> **Note:** I was unable to directly access the GitHub repository (`https://github.com/Manohar-1305/mutating-webhook`) because network access is restricted in this environment. The analysis below is based on the standard structure of Kubernetes mutating admission webhook projects. If the repository deviates from standard patterns, please share the repo files and I'll refine this guide.

A **Mutating Admission Webhook** is a Kubernetes feature that intercepts API requests (like creating a Pod) *before* they are persisted to etcd, and modifies (mutates) them. Common use cases include injecting sidecar containers, adding labels/annotations, setting resource limits, or injecting environment variables.

---

## What a Typical `mutating-webhook` Repository Contains

| File / Folder | Purpose |
|---|---|
| `main.go` (or `app.py`) | The webhook server — listens on HTTPS, receives `AdmissionReview` requests, returns a JSON patch |
| `Dockerfile` | Builds the webhook server into a container image |
| `deploy/` or `k8s/` | Kubernetes manifests: Deployment, Service, MutatingWebhookConfiguration, TLS Secret |
| `webhook.go` / `handler.go` | Core mutation logic — what fields to add/change on incoming objects |
| `generate-certs.sh` | Script to generate TLS certificates (webhooks **must** serve over HTTPS) |
| `README.md` | Project documentation |

---

## How the Mutating Webhook Works (Concept)

```
User runs: kubectl apply -f pod.yaml
          │
          ▼
   ┌─────────────┐
   │  Kubernetes  │
   │  API Server  │──── 1. Receives the Pod creation request
   └──────┬──────┘
          │
          │  2. Checks MutatingWebhookConfiguration
          │     "Is there a webhook registered for Pod CREATE events?"
          │
          ▼
   ┌─────────────────┐
   │ Webhook Server   │──── 3. Receives AdmissionReview (the Pod spec)
   │ (your container) │──── 4. Applies JSON patches (e.g., add a label)
   └──────┬──────────┘──── 5. Returns modified AdmissionReview
          │
          ▼
   ┌─────────────┐
   │  API Server  │──── 6. Persists the MUTATED Pod to etcd
   └─────────────┘
```

**Key point:** The webhook runs as a Pod/Service *inside* the cluster. The API server calls it over HTTPS, so TLS certificates are mandatory.

---

## Prerequisites

Before you begin, ensure you have:

1. **A DigitalOcean account** with billing enabled
2. **`doctl`** — DigitalOcean CLI installed on your local machine
3. **`kubectl`** — Kubernetes CLI installed locally
4. **`docker`** — Docker installed locally (to build the webhook image)
5. **A DigitalOcean Container Registry (DOCR)** — to push your Docker image
6. **`openssl`** — for generating TLS certificates (usually pre-installed on Mac/Linux)
7. **`git`** — to clone the repository

---

## STEP 1: Install and Configure DigitalOcean CLI (`doctl`)

### What you do:
```bash
# Install doctl (macOS)
brew install doctl

# Install doctl (Linux)
sudo snap install doctl

# Authenticate with your DigitalOcean account
doctl auth init
```

### What happens:
- You'll be prompted to enter a **DigitalOcean API token**. Generate one at [https://cloud.digitalocean.com/account/api/tokens](https://cloud.digitalocean.com/account/api/tokens).
- After entering the token, `doctl` stores it locally so all subsequent commands authenticate automatically.

### Expected result:
```
Validating token... ✔
```

---

## STEP 2: Create a DigitalOcean Kubernetes Cluster (DOKS)

### What you do:
```bash
doctl kubernetes cluster create mutating-webhook-cluster \
  --region nyc1 \
  --version latest \
  --size s-2vcpu-4gb \
  --count 2
```

### What each flag means:
- `--region nyc1` — New York datacenter (choose the region nearest to you: `sfo3`, `lon1`, `blr1`, etc.)
- `--version latest` — uses the latest stable Kubernetes version DigitalOcean supports
- `--size s-2vcpu-4gb` — each node gets 2 vCPUs and 4GB RAM (sufficient for testing)
- `--count 2` — creates 2 worker nodes for reliability

### What happens:
- DigitalOcean provisions a managed Kubernetes cluster. This takes **4–8 minutes**.
- `doctl` automatically downloads the kubeconfig and merges it into `~/.kube/config`.
- Your `kubectl` context switches to the new cluster.

### Expected result:
```
Notice: Cluster is provisioning, waiting for cluster to be running
Notice: Cluster created, fetching credentials
Notice: Adding cluster credentials to kubeconfig file found in "/home/you/.kube/config"
Notice: Setting current-context to do-nyc1-mutating-webhook-cluster
```

### Verify:
```bash
kubectl get nodes
```

**Expected output:**
```
NAME                    STATUS   ROLES    AGE   VERSION
pool-xxxx-node-a1b2c   Ready    <none>   2m    v1.29.x
pool-xxxx-node-d3e4f   Ready    <none>   2m    v1.29.x
```

Both nodes should show `Ready`. If they show `NotReady`, wait another minute.

---

## STEP 3: Create a DigitalOcean Container Registry (DOCR)

### What you do:
```bash
# Create a private container registry
doctl registry create my-webhook-registry --subscription-tier starter

# Log Docker into the registry
doctl registry login
```

### What happens:
- Creates a private Docker registry at `registry.digitalocean.com/my-webhook-registry`.
- The `login` command configures Docker to push images to this registry.

### Expected result:
```
Registry created: my-webhook-registry
Login Succeeded
```

### Connect the registry to your Kubernetes cluster:
```bash
doctl kubernetes cluster registry add mutating-webhook-cluster
```

This allows your Kubernetes nodes to pull images from your private registry without needing `imagePullSecrets`.

---

## STEP 4: Clone the Repository

### What you do:
```bash
git clone https://github.com/Manohar-1305/mutating-webhook.git
cd mutating-webhook
```

### What happens:
- Downloads the full source code to your local machine.

### Expected result:
```
Cloning into 'mutating-webhook'...
remote: Enumerating objects: XX, done.
...
```

### Examine the structure:
```bash
ls -la
```

You should see files like `main.go` (or `app.py`), `Dockerfile`, Kubernetes manifests in a `deploy/` or `k8s/` folder, and possibly a certificate generation script.

---

## STEP 5: Generate TLS Certificates

### Why this is needed:
The Kubernetes API server communicates with webhooks over **HTTPS only**. The webhook server must present a valid TLS certificate. The API server validates this certificate using the `caBundle` field in the `MutatingWebhookConfiguration`.

### What you do:

If the repo includes a `generate-certs.sh` script, use it. Otherwise, create certificates manually:

```bash
# Define variables
SERVICE_NAME=mutating-webhook-svc
NAMESPACE=default
SECRET_NAME=webhook-tls-secret

# Generate CA key and certificate
openssl genrsa -out ca.key 2048
openssl req -x509 -new -nodes -key ca.key \
  -subj "/CN=Mutating Webhook CA" \
  -days 3650 -out ca.crt

# Generate server key
openssl genrsa -out server.key 2048

# Create a config file for the server certificate
cat > server.conf <<EOF
[req]
req_extensions = v3_req
distinguished_name = req_distinguished_name
prompt = no

[req_distinguished_name]
CN = ${SERVICE_NAME}.${NAMESPACE}.svc

[v3_req]
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${SERVICE_NAME}
DNS.2 = ${SERVICE_NAME}.${NAMESPACE}
DNS.3 = ${SERVICE_NAME}.${NAMESPACE}.svc
DNS.4 = ${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local
EOF

# Generate server CSR and sign it with our CA
openssl req -new -key server.key \
  -out server.csr \
  -config server.conf

openssl x509 -req -in server.csr \
  -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out server.crt \
  -days 3650 \
  -extensions v3_req \
  -extfile server.conf
```

### What each command does:

| Command | Purpose |
|---|---|
| `openssl genrsa -out ca.key 2048` | Creates a 2048-bit RSA private key for the Certificate Authority |
| `openssl req -x509 ... -out ca.crt` | Creates a self-signed CA certificate (the "trust root") |
| `openssl genrsa -out server.key 2048` | Creates the webhook server's private key |
| `server.conf` | Defines Subject Alternative Names (SANs) — Kubernetes uses these DNS names to reach the webhook |
| `openssl req -new ...` | Creates a Certificate Signing Request for the server |
| `openssl x509 -req ...` | Signs the server certificate with your CA |

### Expected result:
You now have these files:

```
ca.key      ← CA private key (keep secret)
ca.crt      ← CA certificate (goes into MutatingWebhookConfiguration as caBundle)
server.key  ← Server private key (goes into Kubernetes Secret)
server.crt  ← Server certificate (goes into Kubernetes Secret)
```

### Create Kubernetes Secret with the TLS certs:
```bash
kubectl create secret tls webhook-tls-secret \
  --cert=server.crt \
  --key=server.key \
  -n default
```

**Expected result:**
```
secret/webhook-tls-secret created
```

### Get the CA bundle (base64-encoded, for the webhook config):
```bash
CA_BUNDLE=$(cat ca.crt | base64 | tr -d '\n')
echo $CA_BUNDLE
```

Save this value — you'll need it in Step 8.

---

## STEP 6: Build the Docker Image

### What you do:
```bash
# Build the image
docker build -t registry.digitalocean.com/my-webhook-registry/mutating-webhook:v1 .

# Push to DigitalOcean Container Registry
docker push registry.digitalocean.com/my-webhook-registry/mutating-webhook:v1
```

### What happens:
- Docker reads the `Dockerfile`, compiles the webhook server (Go binary or Python app), and packages it into a container image.
- The image is pushed to your private DigitalOcean registry.

### Expected result:
```
Successfully built a1b2c3d4e5f6
Successfully tagged registry.digitalocean.com/my-webhook-registry/mutating-webhook:v1
...
v1: digest: sha256:xxxx... size: 1234
```

### If the build fails:
- Check the `Dockerfile` for missing dependencies.
- If it's a Go project, ensure `go.mod` and `go.sum` are present.
- If it's Python, ensure `requirements.txt` exists.

---

## STEP 7: Deploy the Webhook Server to Kubernetes

### What you do:

Create or edit the deployment manifest. A typical `deployment.yaml` looks like:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mutating-webhook
  namespace: default
  labels:
    app: mutating-webhook
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mutating-webhook
  template:
    metadata:
      labels:
        app: mutating-webhook
    spec:
      containers:
      - name: webhook
        image: registry.digitalocean.com/my-webhook-registry/mutating-webhook:v1
        ports:
        - containerPort: 443
        volumeMounts:
        - name: tls-certs
          mountPath: /etc/webhook/certs
          readOnly: true
      volumes:
      - name: tls-certs
        secret:
          secretName: webhook-tls-secret
---
apiVersion: v1
kind: Service
metadata:
  name: mutating-webhook-svc
  namespace: default
spec:
  selector:
    app: mutating-webhook
  ports:
  - port: 443
    targetPort: 443
```

### Apply it:
```bash
kubectl apply -f deployment.yaml
# OR if the repo has a deploy/ folder:
kubectl apply -f deploy/
```

### What happens:
- Kubernetes pulls the webhook image from DOCR.
- Creates a Pod running the webhook server.
- Creates a Service (`mutating-webhook-svc`) that the API server will call.
- The TLS secret is mounted into the Pod at `/etc/webhook/certs`.

### Expected result:
```bash
kubectl get pods
```
```
NAME                                READY   STATUS    RESTARTS   AGE
mutating-webhook-7f8b9c6d5-x2k4n   1/1     Running   0          30s
```

```bash
kubectl get svc mutating-webhook-svc
```
```
NAME                    TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)   AGE
mutating-webhook-svc    ClusterIP   10.245.x.x      <none>        443/TCP   30s
```

### If the Pod is not Running:
```bash
# Check Pod events
kubectl describe pod -l app=mutating-webhook

# Check container logs
kubectl logs -l app=mutating-webhook
```

Common issues: image pull failures (check registry name), TLS cert mount path mismatches, or the webhook binary crashing on startup.

---

## STEP 8: Register the MutatingWebhookConfiguration

### What you do:

Create `webhook-config.yaml`:

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: MutatingWebhookConfiguration
metadata:
  name: mutating-webhook-config
webhooks:
- name: mutating-webhook.default.svc.cluster.local
  admissionReviewVersions: ["v1", "v1beta1"]
  sideEffects: None
  clientConfig:
    service:
      name: mutating-webhook-svc
      namespace: default
      path: "/mutate"          # The endpoint your webhook server listens on
    caBundle: <PASTE_YOUR_CA_BUNDLE_HERE>   # The base64 CA cert from Step 5
  rules:
  - operations: ["CREATE"]
    apiGroups: [""]
    apiVersions: ["v1"]
    resources: ["pods"]
  failurePolicy: Ignore        # Ignore = don't block if webhook is down
  namespaceSelector:
    matchExpressions:
    - key: kubernetes.io/metadata.name
      operator: NotIn
      values: ["kube-system", "kube-public"]  # Don't mutate system namespaces
```

### Replace the `caBundle`:
```bash
# Use the CA_BUNDLE from Step 5
sed -i "s/<PASTE_YOUR_CA_BUNDLE_HERE>/$CA_BUNDLE/g" webhook-config.yaml
```

### Apply it:
```bash
kubectl apply -f webhook-config.yaml
```

### What each field means:

| Field | Meaning |
|---|---|
| `clientConfig.service` | Tells the API server WHERE the webhook is (service name + namespace + path) |
| `caBundle` | The CA certificate used to verify the webhook's TLS cert |
| `rules` | WHEN to call the webhook — here, only on Pod CREATE operations |
| `failurePolicy: Ignore` | If the webhook is unreachable, allow the request anyway (use `Fail` for strict enforcement) |
| `namespaceSelector` | Excludes system namespaces from mutation |

### Expected result:
```
mutatingwebhookconfiguration.admissionregistration.k8s.io/mutating-webhook-config created
```

### Verify:
```bash
kubectl get mutatingwebhookconfiguration
```
```
NAME                       WEBHOOKS   AGE
mutating-webhook-config    1          10s
```

---

## STEP 9: Test the Webhook

### What you do:

Create a test Pod to see if the webhook mutates it:

```bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: test-pod
  labels:
    app: test
spec:
  containers:
  - name: nginx
    image: nginx:latest
EOF
```

### What happens:
1. `kubectl` sends the Pod creation request to the API server.
2. The API server sees the `MutatingWebhookConfiguration` matches (Pod CREATE).
3. The API server sends an `AdmissionReview` to your webhook at `https://mutating-webhook-svc.default.svc:443/mutate`.
4. Your webhook processes the request and returns a JSON patch (e.g., add labels, inject sidecar).
5. The API server applies the patch and creates the modified Pod.

### Check what was mutated:
```bash
kubectl get pod test-pod -o yaml
```

### Expected result (depends on what the webhook does):

If the webhook adds labels, you'll see something like:
```yaml
metadata:
  labels:
    app: test
    injected-by: mutating-webhook    # ← This was added by the webhook!
```

If it injects a sidecar container:
```yaml
spec:
  containers:
  - name: nginx
    image: nginx:latest
  - name: sidecar               # ← This was injected by the webhook!
    image: busybox
```

### Check webhook logs to see the mutation:
```bash
kubectl logs -l app=mutating-webhook
```

**Expected log output:**
```
Received admission review request for Pod: test-pod
Mutating pod: adding label injected-by=mutating-webhook
Sending response with patch...
```

---

## STEP 10: Troubleshooting Common Issues

### Issue: Pod creation hangs or fails with webhook errors

```bash
# Check the webhook Pod is running
kubectl get pods -l app=mutating-webhook

# Check webhook logs for errors
kubectl logs -l app=mutating-webhook --tail=50

# Check API server can reach the webhook
kubectl describe mutatingwebhookconfiguration mutating-webhook-config
```

### Issue: `x509: certificate signed by unknown authority`

This means the `caBundle` in your `MutatingWebhookConfiguration` doesn't match the CA that signed the server certificate. Re-generate certs (Step 5) and re-apply the webhook config (Step 8).

### Issue: `connection refused` or `no endpoints available`

The Service can't reach the webhook Pod. Check that the Service selector matches the Pod labels, and that the Pod is in `Running` state.

### Issue: Image pull errors

```bash
# Verify DOCR integration
doctl kubernetes cluster registry list

# Re-add if missing
doctl kubernetes cluster registry add mutating-webhook-cluster
```

### Issue: Webhook not being called at all

Check that the `rules` in the `MutatingWebhookConfiguration` match the resource you're creating. Also verify the `namespaceSelector` isn't excluding your namespace.

---

## STEP 11: Cleanup

When you're done testing, clean up to avoid ongoing charges:

```bash
# Delete webhook resources
kubectl delete mutatingwebhookconfiguration mutating-webhook-config
kubectl delete -f deployment.yaml
kubectl delete secret webhook-tls-secret
kubectl delete pod test-pod

# Delete the Kubernetes cluster (stops billing)
doctl kubernetes cluster delete mutating-webhook-cluster --force

# Delete the container registry (optional)
doctl registry delete my-webhook-registry --force
```

### Expected result:
```
Deleting cluster mutating-webhook-cluster... done
```

---

## Quick Reference: Full Command Sequence

```bash
# 1. Setup
doctl auth init
doctl kubernetes cluster create mutating-webhook-cluster --region nyc1 --size s-2vcpu-4gb --count 2
doctl registry create my-webhook-registry --subscription-tier starter
doctl registry login
doctl kubernetes cluster registry add mutating-webhook-cluster

# 2. Clone and build
git clone https://github.com/Manohar-1305/mutating-webhook.git
cd mutating-webhook

# 3. Generate TLS certs (or use repo's script)
# ... (see Step 5 above)
kubectl create secret tls webhook-tls-secret --cert=server.crt --key=server.key

# 4. Build and push image
docker build -t registry.digitalocean.com/my-webhook-registry/mutating-webhook:v1 .
docker push registry.digitalocean.com/my-webhook-registry/mutating-webhook:v1

# 5. Deploy
kubectl apply -f deploy/   # or individual manifests

# 6. Register webhook
kubectl apply -f webhook-config.yaml

# 7. Test
kubectl run test-pod --image=nginx
kubectl get pod test-pod -o yaml | grep -A5 labels

# 8. Cleanup
doctl kubernetes cluster delete mutating-webhook-cluster --force
```

---

## Cost Estimate (DigitalOcean)

| Resource | Monthly Cost (approx.) |
|---|---|
| DOKS cluster (2 nodes, s-2vcpu-4gb) | ~$48/month ($24/node) |
| Container Registry (Starter) | Free (500 MB) |
| **Total for testing** | **~$48/month** |

You can delete the cluster immediately after testing to minimize charges — billing is hourly.
