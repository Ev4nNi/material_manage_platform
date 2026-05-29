# JVM 启动参数说明

## 当前实际生效参数

当前后端镜像通过 `JAVA_TOOL_OPTIONS` 注入 JVM 参数，真实配置以 [Dockerfile](/d:/programProject/workproject/material_manage_platform/backend/Dockerfile:13) 和 [docker-compose.yml](/d:/programProject/workproject/material_manage_platform/docker-compose.yml:1) 为准。

默认值如下：

```text
-Xms256m
-Xmx384m
-XX:MaxMetaspaceSize=128m
-XX:MaxDirectMemorySize=128m
-Xss512k
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/logs/heapdump.hprof
-XX:+ExitOnOutOfMemoryError
```

这组参数是按 `1C2G` 机器估算的默认值，配合容器限制一起使用：

- 后端容器内存上限：`1024m`
- 后端容器 CPU：`1.0`
- 后端容器 `pids_limit`：`256`

## 参数说明

| 参数 | 值 | 作用 |
|------|------|------|
| `-Xms256m` | 256MB | 初始堆内存，降低运行初期频繁扩容带来的抖动 |
| `-Xmx384m` | 384MB | 最大堆内存，控制 Java 堆上限，避免挤占容器全部内存 |
| `-XX:MaxMetaspaceSize=128m` | 128MB | 限制类元数据内存占用 |
| `-XX:MaxDirectMemorySize=128m` | 128MB | 为 Undertow、NIO、文件传输和缓冲区保留堆外内存 |
| `-Xss512k` | 512KB | 收紧单线程栈大小，减少线程总内存占用 |
| `-XX:+UseG1GC` | - | 使用 G1 垃圾回收器 |
| `-XX:MaxGCPauseMillis=200` | 200ms | GC 暂停时间目标 |
| `-XX:+HeapDumpOnOutOfMemoryError` | - | OOM 时自动生成 heap dump |
| `-XX:HeapDumpPath=/data/logs/heapdump.hprof` | 固定路径 | heap dump 保存到容器日志挂载目录 |
| `-XX:+ExitOnOutOfMemoryError` | - | OOM 后直接退出，让 Docker 自动拉起容器 |

## 为什么不用之前的 200MB 小堆方案

旧文档中的 `-Xmx200m` 适合“极限压缩”场景，但不适合当前项目的实际运行负载。这个项目除了常规 Spring Boot 接口外，还有这些额外内存消耗：

- SQLite 页缓存
- Undertow / NIO 直接内存
- 大文件上传
- 图片和视频元数据提取
- 日志缓冲与对象分配

如果继续使用 `Xmx200m`，在批量上传、大图处理或瞬时请求增多时，更容易出现频繁 Full GC、吞吐下降甚至 OOM。

## 容器内存估算

在当前默认配置下，可以粗略按下面估算：

- Java 堆：`384m`
- Metaspace：最多 `128m`
- 直接内存：最多 `128m`
- 线程栈：`512k * 30~50`，大约 `15m ~ 25m`
- SQLite / JVM 本地开销 / 其他缓冲：预留 `80m ~ 150m`

总进程峰值通常可能落在：

```text
735m ~ 815m
```

因此把容器限制在 `1024m` 比较稳，既有边界，又保留了突发余量。

## 当前 Docker 启动方式

后端镜像中的启动方式是：

```sh
exec java -jar app.jar \
  --server.port=${APP_PORT} \
  --server.address=${APP_HOST} \
  --spring.datasource.url=jdbc:sqlite:${DB_PATH} \
  --storage.local.base-path=${STORAGE_PATH} \
  --logging.file.name=${LOG_PATH} \
  --logging.file.max-size=${LOG_MAX_SIZE} \
  --logging.file.max-history=${LOG_MAX_HISTORY} \
  --spring.servlet.multipart.max-file-size=${MAX_FILE_SIZE} \
  --spring.servlet.multipart.max-request-size=${MAX_FILE_SIZE}
```

这里没有把 JVM 参数直接写进 `java` 命令，而是通过 `JAVA_TOOL_OPTIONS` 注入。这样做的好处是：

- Dockerfile 有一套默认值
- `docker-compose.yml` 可以直接覆盖
- 服务器临时调参时不需要改启动脚本

## 覆盖方式

如果需要在不同机器上调整参数，直接修改 `docker-compose.yml`：

```yaml
services:
  backend:
    mem_limit: 768m
    cpus: 0.8
    environment:
      - JAVA_TOOL_OPTIONS=-Xms192m -Xmx320m -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=96m -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/logs/heapdump.hprof -XX:+ExitOnOutOfMemoryError
```

建议遵循这些原则：

- `Xmx` 控制在容器内存的 `35%` 到 `50%`
- `MaxDirectMemorySize` 不要随意去掉
- 如果机器更小，优先先降 `Xmx`，再考虑降 `Xms`
- 不建议把容器内存限制设置得和 `Xmx` 太接近

## 本地手动运行示例

### Windows PowerShell

```powershell
$env:JAVA_TOOL_OPTIONS='-Xms256m -Xmx384m -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=128m -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof -XX:+ExitOnOutOfMemoryError'
java -jar .\target\material-manage-platform-1.0.0.jar
```

### Linux / macOS

```bash
export JAVA_TOOL_OPTIONS="-Xms256m -Xmx384m -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=128m -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof -XX:+ExitOnOutOfMemoryError"
java -jar ./target/material-manage-platform-1.0.0.jar
```

## 排查建议

如果怀疑当前参数仍然偏小，可以优先看这些指标：

- Docker `OOMKilled` 状态
- `/data/logs/heapdump.hprof` 是否生成
- JVM GC 日志或应用卡顿时段
- 大文件上传时的容器内存峰值

如果线上机器更大，不要第一时间把 `Xmx` 拉满；优先根据实际峰值逐步上调。
