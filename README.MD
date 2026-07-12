# Kind Guardian
## Part 1: Local LLM Setup

This phase covers getting a local LLM running via Ollama and wiring into the 
Spring Boot Application. This a foundation - later part will add K8s (Kind) and prometheus integration so that agent can fetch
and diagnose issues

### Goal of Part 1:

Having working /chat endpoint in the spring Boot app that sends message to a local Ollama model and returns its response.

### Prerequisites 

- Windows machine with an NIVDIA GPU (tested on GTX 1050, 4GB VRAM)
- Latest NVIDIA driver installed 
- Intellij IDEA or any IDE.
- Java 17 and Maven Setup for Spring Boot.


#### 1. Install Ollama:
Download and Install from https://ollama.com/download

Verify installation
````
ollama --version
````

#### 2. Pull a model
````
ollama pull <model_name>
````
model used during testing : gemma3:4b

#### 3. Verify Ollama server works standlone
````
ollama run gemma3:4b
````

Type a test Message. If it responds normally the server + GPU setup is working.

#### 4. Spring Boot Setup

Add required dependency in Pom.xml like Spring-AI Ollama and refer application.yml config for local setup.

````
server:
  port: 8081

spring:
  application:
    name: kind-guardian

  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
           model:
              gemma3:4b

````


### 5. Testing the endpoint

- Run Spring boot application
- Make sure Ollama is running
- Crete HTTP request
````
POST http://localhost:8081/chat
Content-Type : application/json

{
  "message": "hello, Are you working?"
}
````

- Expected Response:
````
{
    "response": "Hello there! Yes, I am currently \"working.\" As a large language model, my primary function is to process information and respond to your requests. Right now, I'm responding to *you* – that’s what I’m doing! 😊\n\nIs there anything you'd like me to do or any questions you have?"
}
````

### TroubleShooting

- Error Pull Model manifest : 412 -> Ollama client is outdated. Update via ollama.com/download
- CUDA error : the provided PTX was complied with an unsupported toolchain -> NVIDIA driver is older th9n what Ollama bundled CUDA build expects. Update the driver from nvidia.com/drivers (Game-Ready or studio driver, whichever is newer)