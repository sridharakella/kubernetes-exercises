# Kubernetes Gateway API Setup - Executed Commands

## Complete command sequence for setting up NGINX Gateway Fabric with path-based routing

---

## 1. Initial Setup & Configuration

### Set kubeconfig
```bash
export KUBECONFIG=k8s-sridhar-kubeconfig.yaml
```

---

## 2. Cluster Verification

### Check pods
```bash
 kubectl get pods
```
**Output:** No resources found in default namespace.

### Check all resources
```bash
kubectl get all
```

### Check nodes
```bash
kubectl get nodes
```
**Output:** 2 nodes ready (pool-g1gjxq17y-ktge7, pool-g1gjxq17y-ktgem)

---

## 3. Gateway API Installation

### Install Gateway API CRDs
```bash
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.3.0/standard-install.yaml
```
**Result:** Installed/configured 5 CRDs:
- gatewayclasses.gateway.networking.k8s.io
- gateways.gateway.networking.k8s.io
- grpcroutes.gateway.networking.k8s.io
- httproutes.gateway.networking.k8s.io
- referencegrants.gateway.networking.k8s.io

---

## 4. NGINX Gateway Fabric Installation

### Install NGINX Gateway using Helm
```bash
helm install ngf oci://ghcr.io/nginx/charts/nginx-gateway-fabric --create-namespace -n ng
```
**Result:** 
- Chart version: 2.4.1
- Namespace created: ng
- Status: deployed

### Verify namespace creation
```bash
kubectl get ns
```
**Output:** Shows ng namespace created

---

## 5. Verify Gateway Classes

### List gateway classes
```bash
kubectl get gatewayclasses
```
**Output:** 
- cilium (controller: io.cilium/gateway-controller)
- nginx (controller: gateway.nginx.org/nginx-gateway-controller)

### Get gateway classes with details
```bash
kubectl get gatewayclasses -o wide
```

---

## 6. Deploy Sample Applications

### Deploy blue and green applications
```bash
kubectl apply -f apps.yaml
```
**Result:** Created:
- service/green-svc
- deployment.apps/green-deployment
- service/blue-svc
- deployment.apps/blue-deployment

### Verify deployments
```bash
kubectl get all
```
**Output:** 
- 2 blue pods running
- 2 green pods running
- Services created

### Check services
```bash
kubectl get svc -o wide
```
**Output:**
- blue-svc: ClusterIP 10.109.25.233
- green-svc: ClusterIP 10.109.22.38

---

## 7. Create Gateway

### Apply NGINX Gateway configuration
```bash
kubectl apply -f ngix-gateway.yaml
```
**Result:** gateway.gateway.networking.k8s.io/colors-gateway created

### Monitor gateway resources
```bash
kubectl get all
```
**Shows:**
- colors-gateway-nginx pod creating
- colors-gateway-nginx LoadBalancer service (pending)

### Check service status (multiple times)
```bash
kubectl get svc
```
**Monitoring LoadBalancer IP assignment:**
- Initially: EXTERNAL-IP <pending>
- Eventually: Gets assigned IP

### Verify gateway status
```bash
kubectl get gateway
```
**Output:**
- NAME: colors-gateway
- CLASS: nginx
- ADDRESS: 129.212.240.108
- PROGRAMMED: True

---

## 8. Configure HTTP Routes

### Apply routing rules
```bash
kubectl apply -f routes.yaml
```
**Result:** httproute.gateway.networking.k8s.io/colors-route created

### Verify services with external IP
```bash
kubectl get svc
```
**Output:**
- colors-gateway-nginx: LoadBalancer with EXTERNAL-IP 129.212.240.108

### Check HTTP route
```bash
kubectl get httproute
```
**Output:** colors-route created

### Final gateway check
```bash
kubectl get gateway
```
**Confirms:**
- Gateway programmed and ready
- External IP: 129.212.240.108

---

## 9. Test the Deployment

### Test green service endpoint
```bash
curl http://129.212.240.108/green
```
**Output:**
```
Hello, world!
Version: 1.0.0
Hostname: green-deployment-66b6659994-wc2gj
```
✅ **Success!** Path-based routing working correctly.

---

## Summary of Key Commands

### Quick Reference
```bash
# 1. Set kubeconfig
export KUBECONFIG=k8s-sridhar-kubeconfig.yaml

# 2. Install Gateway API CRDs
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.3.0/standard-install.yaml

# 3. Install NGINX Gateway Fabric
helm install ngf oci://ghcr.io/nginx/charts/nginx-gateway-fabric --create-namespace -n ng

# 4. Deploy applications
kubectl apply -f apps.yaml

# 5. Create gateway
kubectl apply -f ngix-gateway.yaml

# 6. Configure routes
kubectl apply -f routes.yaml

# 7. Get gateway external IP
kubectl get gateway

# 8. Test
curl http://EXTERNAL-IP/green
```

---

## Verification Commands

### Check all resources
```bash
kubectl get all
kubectl get svc
kubectl get gateway
kubectl get httproute
kubectl get gatewayclasses
```

### Check specific namespaces
```bash
kubectl get all -n ng            # NGINX Gateway namespace
kubectl get all -n default       # Application namespace
```

---

## Resource Overview

### Created Resources:
1. **Gateway API CRDs** (5 types)
2. **NGINX Gateway Fabric** (Helm chart in ng namespace)
3. **Applications:**
   - blue-deployment (2 replicas)
   - green-deployment (2 replicas)
   - blue-svc (ClusterIP)
   - green-svc (ClusterIP)
4. **Gateway:**
   - colors-gateway (NGINX class)
   - LoadBalancer service (External IP: 129.212.240.108)
5. **HTTPRoute:**
   - colors-route (path-based routing)

---

## Final Architecture

```
Internet
    ↓
LoadBalancer (129.212.240.108)
    ↓
NGINX Gateway (colors-gateway)
    ↓
HTTPRoute (colors-route)
    ├── /green → green-svc → green-deployment (2 pods)
    └── /blue  → blue-svc  → blue-deployment (2 pods)
```

---

## Notes

- **LoadBalancer IP Assignment:** Took ~30 seconds to provision
- **Gateway Status:** Checked multiple times before IP assignment
- **Gateway Class:** Using NGINX (not Cilium default)
- **Routing:** Path-based routing configured via HTTPRoute
- **Testing:** Successfully accessed green service via external IP

---

## Cleanup Commands (Not Executed)

If you want to tear down:
```bash
# Delete routes
kubectl delete -f routes.yaml

# Delete gateway
kubectl delete -f ngix-gateway.yaml

# Delete applications
kubectl delete -f apps.yaml

# Uninstall NGINX Gateway Fabric
helm uninstall ngf -n ng

# Delete namespace
kubectl delete namespace ng

# Remove Gateway API CRDs (optional, affects all gateways)
kubectl delete -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.3.0/standard-install.yaml
```
