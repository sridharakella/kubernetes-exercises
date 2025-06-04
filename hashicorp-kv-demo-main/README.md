<img width="738" alt="Architecture" src="https://github.com/user-attachments/assets/46756e7a-7eab-424e-8aa4-b5763e79a72a" />



## Hashicorp-kv-demo

Create the vault namespace:

```bash
kubectl create namespace vault
```

Add the HashiCorp Helm repository

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
```

Install HashiCorp Vault using Helm:

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
helm install vault hashicorp/vault \
  --set="server.dev.enabled=true" \
  --set="ui.enabled=true" \
  --set="ui.serviceType=NodePort" \
  --namespace vault
```

Enter inside the vault pod to configure vault with kubernetes

```bash
kubectl exec -it vault-0 -n vault -- /bin/sh
```

Create a policy for reading secrets (read-policy.hcl):

```bash
cat <<EOF > /home/vault/read-policy.hcl
path "secret*" {
  capabilities = ["read"]
}
EOF
```

Write the policy to Vault:

```bash
vault policy write read-policy /home/vault/read-policy.hcl
```

Enable Kubernetes authentication in Vault:

```bash
vault auth enable kubernetes
```

Configure Vault to communicate with the Kubernetes API server

```bash
vault write auth/kubernetes/config \
  token_reviewer_jwt="$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)" \
  kubernetes_host="https://${KUBERNETES_PORT_443_TCP_ADDR}:443" \
  kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
```

Create a role(vault-role) that binds the above policy to a Kubernetes service account(vault-serviceaccount) in a specific namespace. This allows the service account to access secrets stored in Vault

```bash
vault write auth/kubernetes/role/vault-role \
   bound_service_account_names=vault-serviceaccount \
   bound_service_account_namespaces=vault \
   policies=read-policy \
   ttl=1h
```

Create secret using

```bash
vault kv put secret/clisecret token=secretcreatedbycli
```

Verify if secret created or not

```bash
vault kv list secret
```

Check data if secret is injected or not in the pod

```bash
kubectl exec -it <pod name> -n vault -- ls /vault/secrets/
kubectl exec -it <pod name> -n vault -- cat /vault/secrets/clisecret
kubectl exec -it <pod name> -n vault -- cat /vault/secrets/uisecret
```


access the vault UI 

<img width="781" alt="Screen Shot 2025-06-04 at 12 32 01 AM" src="https://github.com/user-attachments/assets/4ee23c73-a6ff-4843-8967-c81da0d1f61d" />

<img width="1158" alt="Screen Shot 2025-06-04 at 12 29 21 AM" src="https://github.com/user-attachments/assets/cd4776ab-71a4-4db7-8fe5-64f6a8dd7b47" />

create a new credentials UI secret username sridhar pwd: (Password) example

<img width="1022" alt="Screen Shot 2025-06-04 at 12 29 37 AM" src="https://github.com/user-attachments/assets/40e81772-c9a6-40b6-959f-7f6a15f068e3" />

verify the creation

<img width="1158" alt="Screen Shot 2025-06-04 at 12 29 21 AM" src="https://github.com/user-attachments/assets/a9c2b487-7460-4f4a-9cec-48a80b22a58a" />

verify the pod to check if the hashcorp secrets are available

<img width="883" alt="Screen Shot 2025-06-04 at 12 29 04 AM" src="https://github.com/user-attachments/assets/b59b20a5-3107-421a-8785-c8b24dc7647f" />



