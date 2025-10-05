# Unicorn Spring AI Agent

A Spring Boot application that uses AWS Bedrock for AI capabilities.

## Prerequisites

- Docker
- Minikube
- kubectl
- AWS credentials configured with access to Bedrock

## Deployment to Minikube

### Option 1: Using the Deployment Script

For a quick and easy deployment, use the provided script:

```bash
./deploy-to-minikube.sh
```

This script will:
1. Start Minikube if it's not running
2. Build the application and Docker image using Spring Boot Maven plugin
3. Load the image into Minikube
4. Extract AWS credentials from your `~/.aws/credentials` or `~/.aws/config` file
5. Create a Kubernetes secret with your AWS credentials
6. Deploy the application to Kubernetes
7. Wait for the deployment to be ready

By default, the script uses the `default` AWS profile. If you want to use a different profile, set the `AWS_PROFILE` environment variable:

```bash
export AWS_PROFILE=your-profile-name
./deploy-to-minikube.sh
```

### Option 2: Manual Deployment

If you prefer to deploy manually, follow these steps:

#### 1. Start Minikube

```bash
minikube start
```

#### 2. Build the application and Docker image

```bash
mvn spring-boot:build-image
```

#### 3. Load the image into Minikube

```bash
minikube image load unicorn-spring-ai-agent:0.0.1-SNAPSHOT
```

#### 4. Configure AWS credentials

The deployment script automatically extracts AWS credentials (access key, secret key, and region) from your `~/.aws/credentials` or `~/.aws/config` file. Make sure you have configured your AWS credentials using the AWS CLI:

```bash
aws configure
```

By default, the script uses the `default` profile. If you want to use a different profile, you can set the `AWS_PROFILE` environment variable:

```bash
export AWS_PROFILE=your-profile-name
./deploy-to-minikube.sh
```

The script will extract the credentials from your AWS config files and create a Kubernetes secret named `aws-credentials`. The deployment will use these credentials to authenticate with AWS services.

#### 5. Deploy the application to Minikube

```bash
kubectl apply -f k8s/deployment.yaml -f k8s/service.yaml -f k8s/configmap.yaml
```

This will deploy:
- The Spring Boot application
- LoadBalancer service to expose the application

### Accessing the Application

#### Option 1: Using Ingress (Recommended when Ingress Addon is Enabled)

If you have the Minikube Ingress addon enabled, you can access the application through the Ingress controller:

```bash
# First, get the IP address of the Minikube node
MINIKUBE_IP=$(minikube ip)
echo $MINIKUBE_IP

# Add an entry to your /etc/hosts file to map unicorn.local to the Minikube IP
# You may need sudo privileges to edit this file
echo "$MINIKUBE_IP unicorn.local" | sudo tee -a /etc/hosts

# Now you can access the application using the hostname
curl -H "Host: unicorn.local" http://$MINIKUBE_IP
# Or open in browser: http://unicorn.local

# You can also access the application directly using the Minikube IP address
# (This works because we've configured the Ingress to handle requests without a host header)
curl http://$MINIKUBE_IP
# Or open in browser: http://$MINIKUBE_IP
```

You can verify that the Ingress is properly configured with:

```bash
kubectl get ingress
```

This should show the unicorn-ingress with the Minikube IP address.

#### Option 2: Using Minikube Tunnel

If you prefer to use a LoadBalancer service, you'll need to run minikube tunnel to assign an external IP:

```bash
# Run this in a separate terminal and keep it running
minikube tunnel
```

This creates a route to services of type LoadBalancer and sets their Ingress to their ClusterIP. After running this command, your service should get an external IP address.

You can check the service status with:

```bash
kubectl get services
```

Once the service has an external IP, you can access it directly:

```bash
# Get the service URL with the assigned IP
SERVICE_URL=$(kubectl get service unicorn-spring-ai-agent -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):8080
echo $SERVICE_URL

# Access the application
curl $SERVICE_URL
# Or open in browser: http://$SERVICE_URL
```

#### Option 3: Using Minikube Service Command

If you don't want to use Ingress or run the tunnel, you can still access the application using:

```bash
minikube service unicorn-spring-ai-agent
```

This command will open the application in your default browser by creating a tunnel to the service's NodePort.

## Kubernetes Resources

- **deployment.yaml**: Deploys the Spring Boot application
- **service.yaml**: Exposes the application with a NodePort service
- **configmap.yaml**: Contains application configuration
- **aws-secret.yaml**: Stores AWS credentials for accessing AWS services
- **ingress.yaml**: Configures the Ingress to route traffic to the service

## Alternative Deployment Using DockerHub

If you're experiencing issues with Minikube pulling the local image, you can push the image to DockerHub first:

1. Build the image with a tag that includes your DockerHub username:

```bash
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=<your-dockerhub-username>/unicorn-spring-ai-agent:0.0.1-SNAPSHOT
```

2. Push the image to DockerHub:

```bash
docker push <your-dockerhub-username>/unicorn-spring-ai-agent:0.0.1-SNAPSHOT
```

3. Update the deployment.yaml to use the DockerHub image:

```yaml
image: <your-dockerhub-username>/unicorn-spring-ai-agent:0.0.1-SNAPSHOT
imagePullPolicy: Always
```

4. Apply the updated deployment:

```bash
kubectl apply -f k8s/deployment.yaml
```

## Troubleshooting

### Check pod status

```bash
kubectl get pods
```

### View pod logs

```bash
kubectl logs -f <pod-name>
```

### Check services

```bash
kubectl get services
```

### Image Pulling Issues

If you see "Back-off pulling image" errors, check:

1. The image tag in deployment.yaml matches what you built
2. The image is available in Minikube's image cache (`minikube image ls`)
3. Consider using the DockerHub approach described above

### AWS Credential Issues

If you see errors like:

```
neither AWS_CONTAINER_CREDENTIALS_FULL_URI or AWS_CONTAINER_CREDENTIALS_RELATIVE_URI environment variables are set
```

This means the container cannot access AWS services because it's missing the necessary credentials. To fix this:

1. Make sure your AWS credentials (including region) are properly configured in your `~/.aws/credentials` or `~/.aws/config` file:
   ```bash
   # Configure your AWS credentials
   aws configure
   ```

2. Run the deployment script again, which will extract your credentials and create the secret:
   ```bash
   ./deploy-to-minikube.sh
   ```

3. If you're using a non-default AWS profile, specify it before running the script:
   ```bash
   export AWS_PROFILE=your-profile-name
   ./deploy-to-minikube.sh
   ```

4. Verify that the deployment is configured to use the secret:
   ```bash
   kubectl describe deployment unicorn-spring-ai-agent
   ```
   You should see environment variables for `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` in the container spec.

5. If you've updated the deployment configuration, apply the changes:
   ```bash
   kubectl apply -f k8s/deployment.yaml
   ```

### LoadBalancer Service in Pending Status

If your service shows `<pending>` for the external IP:

```
unicorn-spring-ai-agent   LoadBalancer   10.98.194.141   <pending>     8080:31315/TCP   14m
```

This is normal behavior in Minikube, as it doesn't provide a built-in load balancer implementation by default. You have three options:

1. **Use Ingress** (recommended if you have the ingress addon enabled):
   ```bash
   # Get the Minikube IP
   MINIKUBE_IP=$(minikube ip)

   # Add an entry to your /etc/hosts file
   echo "$MINIKUBE_IP unicorn.local" | sudo tee -a /etc/hosts

   # Access the application
   curl -H "Host: unicorn.local" http://$MINIKUBE_IP
   # Or open in browser: http://unicorn.local

   # You can also access the application directly using the Minikube IP address
   # (This works because we've configured the Ingress to handle requests without a host header)
   curl http://$MINIKUBE_IP
   # Or open in browser: http://$MINIKUBE_IP
   ```

   You can verify that the ingress is properly configured with:
   ```bash
   kubectl get ingress
   ```

2. **Run minikube tunnel**:
   ```bash
   # Run in a separate terminal and keep it running
   minikube tunnel
   ```
   After running this, your service should get an external IP address.

3. **Use NodePort**:
   You can still access the service using the NodePort that's automatically assigned:
   ```bash
   minikube service unicorn-spring-ai-agent
   ```

The application is deployed with an Ingress resource that routes traffic from the host `unicorn.local` to the service. This is the recommended approach when you have the Minikube Ingress addon enabled, as it doesn't require running the tunnel command in a separate terminal.

## Using the API with curl

You can interact with the application's API using curl. There are multiple ways to access the API depending on how you've set up access to the service:

### Option 1: Using Ingress (Recommended with Ingress Addon)

If you've set up the Ingress as described above, you can access the API using the hostname:

```bash
# Chat API
curl -X POST \
  "http://unicorn.local/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about unicorns"}'

# Streaming Chat API
curl -X POST \
  "http://unicorn.local/api/chat/stream" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about unicorns"}'
```

You can also access the API directly using the Minikube IP address:

```bash
# Get the Minikube IP
MINIKUBE_IP=$(minikube ip)

# Chat API
curl -X POST \
  "http://$MINIKUBE_IP/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about unicorns"}'

# Streaming Chat API
curl -X POST \
  "http://$MINIKUBE_IP/api/chat/stream" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about unicorns"}'
```

### Option 2: Using Minikube Service URL

Alternatively, you can get the service URL using the minikube service command:

```bash
# Get the service URL
SERVICE_URL=$(minikube service unicorn-spring-ai-agent --url)
echo $SERVICE_URL

# Chat API
curl -X POST \
  "$SERVICE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about unicorns"}'

# Streaming Chat API
curl -X POST \
  "$SERVICE_URL/api/chat/stream" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about unicorns"}'
```

### Web Interface

You can also access the web interface by opening the appropriate URL in your browser:

```bash
# Using Ingress with hostname
open http://unicorn.local

# Using Ingress with Minikube IP directly
MINIKUBE_IP=$(minikube ip)
open http://$MINIKUBE_IP

# Or using Minikube service command
minikube service unicorn-spring-ai-agent
```

### Troubleshooting Connectivity Issues

If you're having trouble connecting to the service with curl, try these steps:

#### If `curl http://192.168.49.2` hangs:

This issue occurs when trying to access the service using just the Minikube IP address without configuring the Ingress to handle requests without a host header. To fix this:

1. **Make sure the Ingress is configured to handle requests without a host header**:
   The Ingress should have a rule without a host specified, like this:
   ```yaml
   - http:
       paths:
         - path: /
           pathType: Prefix
           backend:
             service:
               name: unicorn-spring-ai-agent
               port:
                 number: 8080
   ```
   You can check your current Ingress configuration with:
   ```bash
   kubectl get ingress unicorn-ingress -o yaml
   ```

2. **Apply the updated Ingress configuration**:
   ```bash
   kubectl apply -f k8s/ingress.yaml
   ```

3. **Verify the Ingress controller is running**:
   ```bash
   kubectl get pods -n ingress-nginx
   ```

4. **Test the connection again**:
   ```bash
   curl -v http://$(minikube ip)
   ```

If you're still having issues connecting to the service, try the following additional troubleshooting steps:

#### For Ingress-based Access:

1. **Verify the Ingress is properly configured**:
   ```bash
   kubectl get ingress
   ```
   You should see `unicorn-ingress` with the Minikube IP address.

2. **Check your /etc/hosts file**:
   Make sure you have an entry mapping `unicorn.local` to the Minikube IP:
   ```bash
   cat /etc/hosts | grep unicorn.local
   ```
   If it's missing, add it:
   ```bash
   echo "$(minikube ip) unicorn.local" | sudo tee -a /etc/hosts
   ```

3. **Test basic connectivity to the Ingress**:
   ```bash
   # Test with a simple GET request using the host header
   curl -v -H "Host: unicorn.local" http://$(minikube ip)

   # Or test with a simple GET request directly to the IP address
   curl -v http://$(minikube ip)
   ```

4. **Verify the Ingress controller is running**:
   ```bash
   kubectl get pods -n ingress-nginx
   ```
   You should see the ingress-nginx-controller pod running.

#### For Service-based Access:

1. **Verify the service URL is correct**:
   ```bash
   minikube service unicorn-spring-ai-agent --url
   ```

2. **Test basic connectivity**:
   ```bash
   # Test with a simple GET request
   curl -v "$SERVICE_URL"
   ```

3. **Check if the service is accessible on the node port**:
   ```bash
   # Get the node port
   NODE_PORT=$(kubectl get service unicorn-spring-ai-agent -o jsonpath='{.spec.ports[0].nodePort}')
   echo $NODE_PORT

   # Try accessing via node port
   curl -v http://$(minikube ip):$NODE_PORT
   ```

4. **Ensure Minikube tunnel is running** (if needed):
   ```bash
   # In a separate terminal
   minikube tunnel
   ```
   This creates a route to services of type LoadBalancer and sets their Ingress to their ClusterIP.

#### General Troubleshooting:

1. **Check for any network policies or firewalls** that might be blocking the connection.

2. **Verify the application is listening on the correct port**:
   ```bash
   # Check the logs
   kubectl logs -f $(kubectl get pods -l app=unicorn-spring-ai-agent -o jsonpath='{.items[0].metadata.name}')
   ```

3. **Try using the direct URL with the specific endpoints**:
   If you have a URL like http://127.0.0.1:59273, make sure to include the full path to the API endpoint:
   ```bash
   # For the chat API
   curl -X POST \
     "http://127.0.0.1:59273/api/chat" \
     -H "Content-Type: application/json" \
     -d '{"prompt": "Tell me about unicorns"}'

   # For the streaming chat API
   curl -X POST \
     "http://127.0.0.1:59273/api/chat/stream" \
     -H "Content-Type: application/json" \
     -d '{"prompt": "Tell me about unicorns"}'
   ```

4. **Check if the service is accessible from within the cluster**:
   ```bash
   # Start a debug pod
   kubectl run curl-debug --image=curlimages/curl -i --tty --rm -- sh

   # From within the pod, try to access the service
   curl -v http://unicorn-spring-ai-agent:8080
   ```
