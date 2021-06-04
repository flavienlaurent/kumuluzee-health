# KumuluzEE Health
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-health/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-health)

> KumuluzEE Health project provides consistent, unified way of performing microservice health checks and exposing health
  information.

KumuluzEE Health is a health check project for the KumuluzEE microservice framework. It provides easy, consistent and
unified way of performing health checking on microservices and exposing health information to be used by monitoring and
container orchestration environments such as Kubernetes. KumuluzEE Health is fully compliant with Kubernetes and has
been extensively tested to work in Kubernetes.

KumuluzEE Health is compliant with the [MicroProfile Service Health Checks specification 2.2](https://github.com/eclipse/microprofile-health).

KumuluzEE Health exposes `/health/live` and `/health/ready` endpoints (prefix customizable), which return the health
check status of the microservice.

## Usage
You can enable the KumuluzEE Health module by adding the following dependencies:

```xml
<dependency>
    <groupId>com.kumuluz.ee.health</groupId>
    <artifactId>kumuluzee-health</artifactId>
    <version>${kumuluzee-health.version}</version>
</dependency>
```

CDI and JAX-RS dependencies are a prerequisite. Please refer to KumuluzEE [readme](https://github.com/kumuluz/kumuluzee/)
for more information.

## Health checks

To check health of a microservice, you can use the provided health checks or you can define your own health checks.

## Liveness and readiness

KumuluzEE Health differentiates between two health check types - liveness and readiness health check. In short - if a
liveness check fails it means that the service is stuck and should be restarted. If a readiness check fails it means
that the service is temporary unavailable and should not receive requests until all readiness checks succeed.

For more information on liveness and readiness see the following Kubernetes articles:

- [Configure Liveness and Readiness Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/)
- [Container probes](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes)

## Built-in health checks

The following health checks are available out-of-the-box: 

- **DataSourceHealthCheck** for checking the availability of the data source
- **DiskSpaceHealthCheck** for checking available disk space against a threshold
- **ElasticSearchHealthCheck** for checking the availability of Elasticsearch cluster
- **EtcdHealthCheck** for checking the availability of etcd instance
- **HttpHealthCheck** for checking the availability of HTTP resource
- **MongoHealthCheck** for checking the availability of Mongo database
- **RabbitHealthCheck** for checking the availability of RabbitMQ virtual host
- **RedisHealthCheck** for checking the availability of Redis store
- **KafkaHealthCheck** for checking the availability of Kafka cluster

More detailed descriptions of each health check are provided below. Additional built-in health check will be provided
(contributions are welcome).

## Implementing custom health checks

There are two ways how we can implement a custom health check.
* We can use the `@Liveness` and `@Readiness` annotation to define health check classes.
* We can implement health check classes and register them manually.

### @Liveness and @Readiness annotation

To implement health checks using `@Liveness` or `@Readiness` annotation, we have to implement a CDI bean class which
implements the `HealthCheck` interface. Such health checks are automatically discovered and registered to the `HealthRegistry`.

Shown below is an example of a CDI bean health check using `@Readiness` annotation:

```java
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.enterprise.context.ApplicationScoped;

@Readiness
@ApplicationScoped
public class SuccessfulHealthCheckBean implements HealthCheck {

    public HealthCheckResponse call() {
        return HealthCheckResponse.up(SuccessfulHealthCheckBean.class.getSimpleName());
    }

}
```

A health check can also be annotated with both `@Liveness` and `@Readiness` at the same time.

### Health check implemented as class

To implement a health check with a custom class, the class has to implement the `HealthCheck` interface. Such class has
to be manually registered with the `HealthRegistry`.

Shown below is a custom health check implementation. It checks if the KumuluzEE GitHub page is accessible. 

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class GithubHealthCheck implements HealthCheck {

    private static final String url = "https://github.com/kumuluz/kumuluzee";

    private static final Logger LOG = Logger.getLogger(GithubHealthCheck.class.getSimpleName());

    @Override
    public HealthCheckResponse call() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");

            if (connection.getResponseCode() == 200) {
                return HealthCheckResponse.up(GithubHealthCheck.class.getSimpleName());
            }
        } catch (Exception exception) {
            LOG.severe(exception.getMessage());
        }
        return HealthCheckResponse.down(GithubHealthCheck.class.getSimpleName());
    }
}
```

### Registering custom health checks

To register a custom health check class we have to use the `HealthRegistry` instance. We provide the health check unique
name, an instance of the health check class ant the health check type (liveness/readiness).

```java
HealthRegistry.getInstance().register(GithubHealthCheck.class.getSimpleName(), new GithubHealthCheck(), HealthCheckType.LIVENESS);
```

### Unregistering custom health checks

To unregister custom health checks we can use the `HealthRegistry` instance and provide the health check unique name.

```java
HealthRegistry.getInstance().unregister(GithubHealthCheck.class.getSimpleName());
```

## Retrieving health check results

To invoke the health check and retrieve the result we can use the `HealthRegistry` instance. The results will be
returned in a list of health check responses. The desired health check type must also be specified.

```java
List<HealthCheckResponse> results = HealthRegistry.getInstance().getResults(HealthCheckType.BOTH);
```

## /health/* endpoint output

The `/health/live` and `/health/ready` endpoints return:

- 200 with payload, when health checks are defined with positive status or are not defined
- 503 with payload, when health checks are defined, but at least one status is negative
- 500 without payload, when an exception occurred in the procedure of health checking

The health check is available on `http://IP:PORT/health/live` and `http://IP:PORT/health/ready` by default, payload
example is provided below:

```json
{
  "status" : "UP",
  "checks" : [ {
    "name" : "DataSourceHealthCheck",
    "status" : "UP"
  }, {
    "name" : "DiskSpaceHealthCheck",
    "status" : "UP"
  }, {
    "name" : "ElasticSearchHealthCheck",
    "status" : "UP"
  }, {
    "name" : "EtcdHealthCheck",
    "status" : "UP",
    "data": {
      "http://localhost:2379": "UP" 
    }
  }, {
    "name" : "HttpHealthCheck",
    "status" : "UP",
    "data": {
      "https://github.com/kumuluz/kumuluzee-health": "UP"
    }
  }, {
    "name" : "MongoHealthCheck",
    "status" : "UP"
  }, {
    "name" : "RabbitHealthCheck",
    "status" : "UP"
  }, {
    "name" : "RedisHealthCheck",
    "status" : "UP"
  } ]
}
```

The URL also accepts a query parameter `pretty=false` (http://IP:PORT/health/ready?pretty=false) which results in a single
line response, payload example is provided below:

```json
{"status":"UP","checks":[{"name":"DataSourceHealthCheck","status":"UP"},{"name":"DiskSpaceHealthCheck","status":"UP"},{"name":"ElasticSearchHealthCheck","status":"UP"},{"name":"EtcdHealthCheck","status":"UP","data":{"http://localhost:2379": "UP"}},{"name":"HttpHealthCheck","status":"UP","data":{"https://github.com/kumuluz/kumuluzee-health":"UP"}},{"name":"MongoHealthCheck","status":"UP"},{"name":"RabbitHealthCheck","status":"UP"},{"name":"RedisHealthCheck","status":"UP"}]}
```

## Configuring health check endpoint prefix

Health check is provided via URL, the health servlet is registered automatically on path `/health/*`. To configure the
health check endpoint prefix, you can specify the following configuration keys:

- `kumuluzee.health.servlet.mapping`: Health servlet path. Default value is `/health/*`.
- `kumuluzee.health.servlet.enabled`: Is JSON output enabled. Default value is `true`. If false only the status codes
  will be provided.

The JSON output will also be enabled if the DEBUG mode is enabled, by setting `kumuluz.debug` to true.

Example of the configuration:

```yaml
kumuluzee:
  health:
    servlet:
      mapping: /my-health
      enabled: true
```

## Enabling health check logging

Periodic logging of health check results is also available. To configure the health check results logging, you can
specify the following configuration keys:

- `kumuluzee.health.logs.enabled`: Is logging enabled. Default value is `true`.
- `kumuluzee.health.logs.type`: Type of health checks to be logged. Allowed values: `both`, `readiness`, `liveness`.
  Default value is `both`.
- `kumuluzee.health.logs.level`: The logging level. Default value is `FINE`.
- `kumuluzee.health.logs.period-s`: The logging period in seconds. Default value is `60`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    logs:
      enabled: true
      type: readiness
      level: FINE
      period-s: 60
```

## Disabling health extension

Health extension can be disabled by setting the configuration property `kumuluzee.health.enabled` to `false`. This will
disable the `/health` endpoint and disable health check logging.

## Configuring built-in health checks

To configure built-in health checks, we can use the configuration parameters listed below for each built-in health check.
Every built-in health check supports a configuration key `type` which specifies under which type the health check should
be registered. Allowed values are `liveness`, `readiness` and `both`. Default value is `readiness`. For example see the
below configuration of _DataSourceHealthCheck_. 

### DataSourceHealthCheck

To enable data source availability health check, we need to provide in the health check sections. `Jndi-name`,
`connection-url`, `username` and `password` need to be provided as part of the health check configuration. Note that 
multiple data source health checks are supported.

Example configuration:

```yaml
kumuluzee:
  datasources:
    - jndi-name: jdbc/CustomersDS
      connection-url: jdbc:postgresql://localhost:5432/customers
      username: postgres
      password: postgres
      pool:
      	max-size: 20
    - jndi-name: jdbc/OrdersDS
      connection-url: jdbc:postgresql://localhost:5433/orders
      username: postgres
      password: postgres
      pool:
      	max-size: 20
  health:
    checks:
      data-source-health-checks
        - jndi-name: jdbc/CustomersDS
	  type: liveness
	- jndi-name: jdbc/OrdersDS
	  type: liveness
```

Another example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      data-source-health-check:
        connection-url: jdbc:db2://localhost:5021/customers
        username: db2
        password: db2
```

Another example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      data-source-health-check:
        connection-url: jdbc:mysql://localhost:3306/customers?user=mysql&password=mysql
```

To enable data source availability health check, we also need to provide a database driver library in pom.xml.

Example configuration:

```xml
<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
	<version>42.0.0</version>
</dependency>
```

### DiskSpaceHealthCheck

To enable disk space health check, we need to provide the health check config parameters, listed below. The default disk
space threshold is `100MB`, but can be overwritten by providing your own threshold.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      disk-space-health-check:
        threshold: 100000000
```

### ElasticSearchHealthCheck

To enable Elasticsearch cluster health check, we need to specify the `connection-url` with cluster health check endpoint
as part of the health check configuration. The cluster health check endpoint is typically available on
`http://HOST:IP/_cluster/health`. The response should resemble:

```json
{
  "cluster_name" : "testcluster",
  "status" : "yellow",
  "timed_out" : false,
  "number_of_nodes" : 1,
  "number_of_data_nodes" : 1,
  "active_primary_shards" : 5,
  "active_shards" : 5,
  "relocating_shards" : 0,
  "initializing_shards" : 0,
  "unassigned_shards" : 5,
  "delayed_unassigned_shards": 0,
  "number_of_pending_tasks" : 0,
  "number_of_in_flight_fetch": 0,
  "task_max_waiting_in_queue_millis": 0,
  "active_shards_percent_as_number": 50.0
}
```

ElasticSearchHealthCheck checks if the status of HTTP response is 200 and if status field is either `green` or `yellow`.
The default connection-url is `http://localhost:9200/_cluster/health`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      elastic-search-health-check:
        connection-url: http://localhost:9200/_cluster/health?pretty
```

### EtcdHealthCheck

To enable etcd health check, we need to specify the `connection-url` or multiple `connection-url` as part of the health
check configuration.

Example configuration:

```yaml
kumuluzee:
  health:
    checks:
      etcd-health-check:
        connection-url: http://localhost:2379/health
```

Another example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      etcd-health-check:
        - connection-url: http://localhost:2379/health
        - connection-url: http://192.168.99.100:2379/health
```

### HttpHealthCheck

We can provide single or multiple urls for HTTP availability health check. To enable HTTP availability health check, we
need to specify the `connection-url` or multiple `connection-url` as part of the health check configuration. During the
http health check HEAD requests are made to all the `connection-url` and status code is verified if its >=200 and <300.

Example configuration:

```yaml
kumuluzee:
  health:
    checks:
      http-health-check:
        connection-url: https://github.com/kumuluz/kumuluzee-health
```

Another example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      http-health-check:
        - connection-url: https://github.com/kumuluz/kumuluzee-health
        - connection-url: http://www.reddit.com
```

### MongoHealthCheck

To enable the Mongo database health check, we need to provide the `connection-url` config parameter with user, password,
database name and other options need to be provided as part of the health check configuration as described in the
[mongo-java-driver-documentation](https://mongodb.github.io/mongo-java-driver/3.5/javadoc/com/mongodb/MongoClientURI.html).
The default connection-url is `mongodb://localhost:27017/local?serverSelectionTimeoutMS=2000`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      mongo-health-check:
        connection-url: mongodb://user:password@localhost:27017/customers?serverSelectionTimeoutMS=2000
```

To enable the Mongo database health check, we also need to provide mongo-java-driver library in pom.xml.

Example configuration:

```xml
<dependency>
	<groupId>org.mongodb</groupId>
	<artifactId>mongo-java-driver</artifactId>
	<version>3.9.1</version>
</dependency>
```

### RabbitHealthCheck

To enable RabbitMQ health check, we need to specify the `connection-url` with port, username, password and virtual host
as part of the health check configuration. The default connection-url is
`amqp://guest:guest@localhost:5672?connection_timeout=2000`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      rabbit-health-check:
        connection-url: amqp://guest:guest@localhost:5672/virtualHost?connection_timeout=2000
```

To enable RabbitMQ health check, we also need to provide amqp-client library in pom.xml.

Example configuration:

```xml
<dependency>
	<groupId>com.rabbitmq</groupId>
	<artifactId>amqp-client</artifactId>
	<version>5.6.0</version>
</dependency>
```

### RedisHealthCheck

To enable Redis store health check, we need to specify the `connection-url` with port, secret and database number as
part of the health check configuration. The default connection-url is `redis://localhost:6379/0`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      redis-health-check:
        connection-url: redis://:secret@localhost:6379/0
```

To enable Redis store health check, we also need to provide jedis library in pom.xml.

Example configuration:

```xml
<dependency>
	<groupId>redis.clients</groupId>
	<artifactId>jedis</artifactId>
	<version>3.0.1</version>
</dependency>
```

### KafkaHealthCheck

To enable Kafka cluster health check provide the `bootstrap-servers` urls as a comma separated list. The default value
is `localhost:9092`.

You can supply the `minimum-available-nodes` value (default is `1`). The value represents the minimum number of
available nodes in the cluster in order for the health check to be considered successful.

Example of the configuration:

```yml
kumuluzee:
  health:
    checks:
      kafka-health-check:
        bootstrap-servers: localhost:9095,localhost:9096,localhost:9097
        minimum-available-nodes: 2
        request-timeout-ms: 1000
```

Additionally, all properties defined in the subtree `kumuluzee.health.checks.kafka-health-check` are forwarded to Kafka
client (see [CONFGIGURATION](https://kafka.apache.org/documentation/#configuration)). Note that dots (`.`) are replaced
with minuses (`-`), e.g. `request.timeout.ms` becomes `request-timeout-ms`.

The following dependency needs to be provided in order for the health check to function correctly
(when using `kumuluzee-streaming` library the dependency is already provided):

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>2.6.0</version>
</dependency>
```

## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-health/releases)

__NOTE FOR 1.x USERS__: The `/health` endpoint and the `@Health` annotation are deprecated as of 2.0.0 release. They
still work as before but should not be used for any new services. Also note that property names of the JSON structure
were changed in the 2.0.0 release.

## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-health/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the [guidelines](https://github.com/kumuluz/kumuluzee-health/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
