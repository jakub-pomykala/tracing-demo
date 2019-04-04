[![Build Status](https://travis-ci.org/IBM/troubleshoot-with-opentracing-and-istio.svg?branch=master)](https://travis-ci.org/IBM/troubleshoot-with-openrtracing-and-istio)


# Use Distributed Tracing and OpenTracing in Istio to aid in microsorvice deployment troubleshooting

The movement toward microservice architectures has grown hand in hand with the growth of the public cloud. The two trends work together to allow organizations to neatly divide functionality. This helps with scale software function and engineering teams, as individual functions can be built and deployed independently into a container orchesteration system that manages many infrastructure issues with relative ease. 


[Istio](https://istio.io/), a joint collaboration between IBM, Google and Lyft provides an easy way to create a service mesh that will manage many of these complex tasks automatically, without the need to modify the microservices themselves. Istio does this by:

## Included Components
- [Istio](https://istio.io/)
- [IBM Cloud Kubernetes Service](https://console.ng.bluemix.net/docs/containers/cs_ov.html#cs_ov)
- [Jaeger](https://www.jaegertracing.io/)

# Prerequisite
Create a Kubernetes cluster with either [Minikube](https://kubernetes.io/docs/getting-started-guides/minikube) for local testing or with [IBM Cloud Kubernetes Service](https://console.ng.bluemix.net/docs/containers/cs_ov.html#cs_ov) to deploy in cloud. Once your cluster is up, install [Helm](https://helm.sh/docs/using_helm/), which is required for Istio installation.

# Setup

Let's install the Istio service mesh installed on top of your Kubernetes cluster. 

```bash
$ curl -L https://git.io/getLatestIstio | sh -
$ mv istio-<version> istio # replace with version downloaded
$ kubectl create namespace istio-system
$ kubectl apply -f install/kubernetes/helm/helm-service-account.yaml
$ helm init --service-account tiller
```

When installing Istio, you are able install either a [Jaeger](https://www.jaegertracing.io/) or [Zipkin](https://zipkin.io/) tracing back-end. Most of the steps are the same with either service:

To install with a Jaeger back-end, run:

```
helm install kubernetes/helm/istio --name istio --namespace istio-system  --set grafana.enabled=true --set servicegraph.enabled=true --set tracing.enabled=true
```

for Zipkin:

```
helm install kubernetes/helm/istio --name istio --namespace istio-system  --set grafana.enabled=true --set servicegraph.enabled=true --set tracing.enabled=true --set tracing.provider=zipkin
```


Accessing the tracing dashboards:


- Clone and check out the sample application:


Create a working directory to clone this repo and to download Istio into: (note: will be moved to this repository after review)

```bash
$ mkdir ibm
$ cd ibm
$ git clone https://github.com/IBM/opentracing-istio-troubleshooting 
```

Run the deployment scripts. The Docker images for this pattern are already uploaded to Docker hub. 

Note: need section on rebuilding Liberty services code/images and pushing custom images to your Docker hub repo.

Istio mesh promises to add observability to the complex task of microservice deployments. Although Istio simplifies some aspects, the added function is not completely "free" in the sense that, as we'll see,
an application inside the mesh does need to do some extra work to propagage request context through the mesh. However, OpenTracing support in Open Liberty reduces the cost of providing a mesh-wide view of distributed tracing. 
It allows basic support for distributed tracing without making any code changes. As a baseline, Istio must be installed with tracing enabled and, for the purposes of this demo, the sampling frequencing will be increased from the default 1% to something higher so we can see more frequent traces. The question here is, what do we learn without touching the code? 

One of the perhaps confusing areas of Istio is whether and how much an application has to be changed to advantage of the distributed tracing support.  If you install the standard "Bookinfo" application, you will find that requests are traced and visible in Zipkin or Jaeger.  However, each part of the app (links here) has code to capture and propagate context across calls to enable linking of calls along requests (Documentation)  https://istio.io/docs/tasks/telemetry/distributed-tracing/#understanding-what-happened. As you will notice in the Bookinfo app, manually handling context propagation for the purposes of tracing is tedious, and more importantly, it's easy to make an omission and leave the headers out of a REST call, creating gaps in the trace record. This is where Liberty microprofile comes in. When using JAX-RS REST client alongside microprofile OpenTracing, all context propagation is handled automatically. Developers of new services don't have any need for custom trace propagation code.  Global automatic propogation removes the opportunity for a deveoloper to forget to include instrumentation, thus causing gaps in the tracing data.

In this pattern we walk through a way to do distributed tracing in a multi-service application most of the microservices are running OpenLiberty with Microprofile, a set of technologies intended to simplfy cloud-native deployments. We add a Node microservice not using OpenTracing to illustrate how the Istio mesh enhances observability in a polyglot environment and as an interesting contrast with OpenLiberty's code-free instrumentation of RPCs.  The sample application is extremely simple and has the smallest amount of code possible to demonstrate distributed tracing in the context of a service mesh. In a few places the application will throw synthetic exceptions to give us something to look for in the trace data. There is also simulated delay of operations to force some respoinses to take longer than others.  I want to emphasize that the app does not do any actual processing.

We'll walk through the process of these steps:

- Installing Istio with tracing enabled
- Add OpenTracing features on an OpenLiberty project
- run a simple microservice based application and explore what we can learn from distributed tracing UI (Jaeger)
- inject several different kinds of failures into the application and use distributed tracing to investigate the root cause.

# Architecture

The app is made up of six services:


![arch](images/macimg/diag3.png)

After a user sends a POST call into the ingress gateway public IP address, that request flows through the sample app as follows:

1. The Istio ingress gateway forwards the request to the service registered under the `instrumennt-craft-shop` name.
2. The `instrumennt-craft-shop` service calls to the `maker-bot` service, which then:
3. kicks off the "processing pipeline", which consists of four steps, each running in a separate pod.
4. the `maker-bot` service waits for the entire pipeline to complete.
5. If the pipeline completes, the final step in the sequence is a call from the `maker-bot` to the `dbwrapper` service, which, in a real service could persist the object to a database, but in our case sleeps for a short period of time before returning a response.

The processing pipeline represents any service with multiple steps (e.g. ETL workflow) to allow us to show the value of using distributed tracing in such an architecture. The sample app does not actually do any data processing, of course, since it exists to create a source of (occasional) error conditions that we can view in a trace.   Anyone who has worked with an architecture that could remotely be described as microservice will be familiar with the experience of seeing failure or sub-optimal performance and wondering what the root cause could be.  For simplicity, the service flow is synchronous. 

# Green path through application - no errors

Let's take a look at one succesfull request.

First, let's get the external IP and port of the ingress gateway:

```
$  kubectl get svc -n istio-system istio-ingressgateway
NAME                   TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)                                                                                                                   AGE
istio-ingressgateway   LoadBalancer   172.21.158.107   169.55.65.202   80:31380/TCP,443:31390/TCP,31400:31400/TCP,15011:31315/TCP,8060:31977/TCP,853:31762/TCP,15030:30709/TCP,15031:31912/TCP   26d
```

Then, we'll make a single REST call to our service via the ingress IP/port:

`curl 169.55.65.202:31380/instrument-craft-shop/resources/instruments -i -XPOST -H 'count: 1'  -H 'Content-Type: application/json' -d '{"type":"GUITAR", "price":200}'`


Heading over to the dashboard, we'll see a trace that looks like this. Notice that we see the total time for the request (3.5s) and have separate spans for work done in each service.  Because both Microprofile OpenTracing and the Envoy proxy (sidecar container) are sending traces to the collector, spans for both show up and are nearly identical in length, as the proxy adds very little latency to each call. (Since the JAX-RS integration with Microprofile OpenTracing propogates the `x-b3-traceid` header value across network calls, the trace collector is able to combine information from both services.  This is also the reason our Node service (`pipeline-js` in the diagram here) is made part of this collection: even though it's not using an OpenTracing (or any tracing library) for that matter, we're able to see the request to the Node service in the flow of the due to the mpOpenTracing automatically propogating HTTP headers across calls. 

![arch](images/macimg/full-span-no-error.png)

# Microprofile Open Tracing vs. manual context propogation:

Without a tracing library, headers in the Node service need to be copied from input to output to maintain the unified trace context:


```javascript
    var b3headers = ["x-b3-traceid",
	                 "x-b3-spanid",
					 "x-b3-sampled",
					 "x-b3-parentspanid"];
    axios.defaults.headers.post = {};
    b3headers.forEach(function(hdr) {
        var value = req.headers[hdr];
        if (value) {
            axios.defaults.headers.post[hdr] = value;
        }
      });
```

When viewed in Jaeger or Zipkin, the Node service is still visible among the Java applications, since the traceid is consistent with trace IDs propogated by JAX-RS/mpOpenTracing, allowing it to be placed in context of other services. Every time a network call is made, these headers must be propogated, either manually or through a library that wraps network calls.

OpenLiberty changes to build OpenLiberty Docker containers that enable trace propagation:

In the server.xml file, add these two features.  microProfile-2.1 includes JAX-RS and mpOpenTracing, among others.  The Zipkin feature provides support for Zipkin compatible trace collector.  The `opentracingZipkin` 

```
   <featureManager>
      <feature>microProfile-2.1</feature>
     <feature>usr:opentracingZipkin-0.31</feature>
    </featureManager>

    <opentracingZipkin host="zipkin.istio-system" port="9411" />

```

In the Dockerfile, we are copying the `lib` directory that includes the zipkin user feature downloaded by Maven:

```
COPY target/liberty/wlp/usr/extension /opt/ol/wlp/usr/extension/
```

These sections in the `pom.xml` are necessary to download the zipkin user feature (see the full `pom.xml` here: [link to github])

```
    <properties>
        <zipkin.usr.feature>https://github.com/WASdev/sample.opentracing.zipkintracer/releases/download/1.2/liberty-opentracing-zipkintracer-1.2-sample.zip</zipkin.usr.feature>
    </properties>
```

And the plugin to download the user feature:

```
        <plugin>
          <groupId>com.googlecode.maven-download-plugin</groupId>
          <artifactId>download-maven-plugin</artifactId>
          <version>${version.download-maven-plugin}</version>
          <executions>
            <execution>
              <id>install-tracer</id>
              <phase>prepare-package</phase>
              <goals>
                <goal>wget</goal>
              </goals>
              <configuration>
                <url>${zipkin.usr.feature}</url>
                <unpack>true</unpack>
                <outputDirectory>${project.build.directory}/liberty/wlp/usr</outputDirectory>
              </configuration>
            </execution>
          </executions>
        </plugin>
```

Using the JAX-RS client library - `javax.ws.rs.client.Client` and related classes - create new spans within the collector, in addition to preserving trace id header across calls.

# Load testing with Artillery

We'll use Artillery (http://artillery.io) `$ npm install -g artillery`, a service load testing tool to drive many requests to our cluster. Once the run is complete, we can examine the distributed tracing dashboard to understand.


```
$ artillery run load-test.yml 

All virtual users finished
Summary report @ 14:59:53(-0400) 2019-04-04
  Scenarios launched:  28
  Scenarios completed: 28
  Requests completed:  28
  RPS sent: 0.82
  Request latency:
    min: 747.1
    max: 25311.3
    median: 8253.1
    p95: 24145.1
    p99: 25311.3
  Scenario counts:
    build instrument: 28 (100%)
  Codes:
    204: 20
    500: 8

```

This launched 28 requests over the course of 10 seconds. Looks like our built-in failure clauses impacted one of the services 20 times. A snapshot shows how long each request took, and we can even examine the outliers by clicking on them (red arrow).

![arch](images/macimg/artillery-result1.png)

The result shows us that the `pipeline-n1` process had an extremely long runtime, ultimately resulting in a network timeout upstream:

![arch](images/macimg/timeout1.png)

Upstream timeout.  Note the `Logs` is available due to JAX-RS instrumentation through OpenLiberty tracing instrumentation, which is not available directly through Envoy.

![arch](images/macimg/timeout2.png)

If we follow the `x-request-id` value from the envoy trace entry for that process, we can use that to locate the specific invcation of this request. 

![arch](images/macimg/timeout3.png)

Normally, you would want to aggregate your logs in a central location [link to Cloud LogDNA service here] but for the purpose of our demo, we can see the request is taking 19370ms, causing a connection error in the top level `maker-bot` service, which has client connections set to timeout after 20 seconds.

```

$ kubectl logs pipeline-n1-694c45bb8b-8nlns pipeline-node | grep -C6 aefd6fa9-635b-9a83-895a-c5f3e3d14225
Accept: */*
User-Agent: Apache-CXF/3.3.0
Cache-Control: no-cache
Pragma: no-cache
Content-Length: 42
X-Forwarded-Proto: http
x-request-id: aefd6fa9-635b-9a83-895a-c5f3e3d14225
x-envoy-decorator-operation: pipeline-n1.default.svc.cluster.local:9080/*
x-istio-attributes: Cj8KCnNvdXJjZS51aWQSMRIva3ViZXJuZXRlczovL21ha2VyLWJvdC02NGZmYmM3YzY1LXp6ZzVqLmRlZmF1bHQKPgoTZGVzdGluYXRpb24uc2VydmljZRInEiVwaXBlbGluZS1uMS5kZWZhdWx0LnN2Yy5jbHVzdGVyLmxvY2FsCkEKF2Rlc3RpbmF0aW9uLnNlcnZpY2UudWlkEiYSJGlzdGlvOi8vZGVmYXVsdC9zZXJ2aWNlcy9waXBlbGluZS1uMQpDChhkZXN0aW5hdGlvbi5zZXJ2aWNlLmhvc3QSJxIlcGlwZWxpbmUtbjEuZGVmYXVsdC5zdmMuY2x1c3Rlci5sb2NhbAoqCh1kZXN0aW5hdGlvbi5zZXJ2aWNlLm5hbWVzcGFjZRIJEgdkZWZhdWx0CikKGGRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZRINEgtwaXBlbGluZS1uMQ==
doWork process time = 19370 ms
[err]   at javax.servlet.http.HttpServlet.service(HttpServlet.java:706)
[err]   at com.ibm.websphere.jaxrs.server.IBMRestServlet.service(IBMRestServlet.java:96)
[err]   at [internal classes]
```

With the help of distributed tracing, the entire process of finding the longest running request and identifying its error took seconds.

# Failure scenarios that can be discovered explore with distributed tracing

- Slow performance
- network timeouts
- service not started
- incorrect URLs
- NPE and other exceptions during operation

Now, the `x-request-id` value is injected by Istio header can be forwarded as well, but if untouched I find it helpful if logging it to have a correlation id to tie into individual requests, as tracing does not record detailed error data, for that, you would need to fall back to your log aggregation service (link to LogDNA in IBM cloud)

https://www.envoyproxy.io/docs/envoy/latest/intro/arch_overview/tracing#arch-overview-tracing


Here I want to discuss a specific use case for distributed tracing. Let's take a look at a simple microservice based sample app and if we can discover problems. In the app, basd on a demo app called "instrument-craft-shop" it simulates an interface to a manufacturing facility. But the details are not important for the demonstration. The key here is that it's an application whose function is distributed across several microservices, each on running in an OpenLiberty container. 


Steps
Installation of Istio (TBD)


Let's run a load tester against the app and see what we can learn from the story through distributed tracing.

Artillery [ link to artillery here ]

Hint: Because the traces don't retain information about path parameters or payload bodies, separating REST operations by URL helps to identify what was going on by quickly glancing at the trace UI.

No tracing
-------------

![notrace](images/macimg/zipkin-notrace-indivspan.png)

Scenarios:
-----------

1. Service not yet started:
------------------------

To begin with, we'll look at a relatively simple situation: service is unavailable.
In a microservice environment, sometimes, a service isn't ready, or has failed for some purpose. Another service attempting to call this service will get an error.  This jumps out immediately in the distributed tracing
system as some services just don't appear.  In this case, a pipeline node did not complete its startup and the web application was not ready to receive requests.  We can see it's missing from the trace entirely and if we dig deeper we find a "404" message in the maker-bot:

![4041](images/macimg/404-service-not-started-1.png)
![4041](images/macimg/404-service-not-started-2a.png)

# Steps

# References
[Istio.io](https://istio.io/docs/tasks/)
# License
[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
