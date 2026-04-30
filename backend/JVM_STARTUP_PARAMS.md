# JVM 启动参数配置（200MB 内存限制）

## 推荐启动参数

```
-Xms64m -Xmx200m -XX:MaxMetaspaceSize=64m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof
```

## 参数说明

| 参数 | 值 | 说明 |
|------|------|------|
| `-Xms64m` | 64MB | 初始堆内存，设置较小值以节省启动内存 |
| `-Xmx200m` | 200MB | 最大堆内存，硬性限制防止内存溢出 |
| `-XX:MaxMetaspaceSize=64m` | 64MB | 元空间上限，控制类元数据内存 |
| `-XX:+UseG1GC` | - | 使用 G1 垃圾收集器，适合小内存场景 |
| `-XX:MaxGCPauseMillis=200` | 200ms | 目标 GC 暂停时间，平衡吞吐量与响应 |
| `-XX:+HeapDumpOnOutOfMemoryError` | - | OOM 时自动生成堆转储，便于排查 |
| `-XX:HeapDumpPath` | ./logs/ | 堆转储文件保存路径 |

## 完整启动命令

### Windows (PowerShell / CMD)

```powershell
java -Xms64m -Xmx200m -XX:MaxMetaspaceSize=64m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof -jar material-manage-platform-1.0.0.jar
```

### Linux / macOS

```bash
java -Xms64m -Xmx200m -XX:MaxMetaspaceSize=64m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof -jar material-manage-platform-1.0.0.jar
```

### 使用 spring-boot-maven-plugin 运行

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms64m -Xmx200m -XX:MaxMetaspaceSize=64m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## 环境变量方式

```bash
# Windows
set JAVA_OPTS=-Xms64m -Xmx200m -XX:MaxMetaspaceSize=64m -XX:+UseG1GC

# Linux / macOS
export JAVA_OPTS="-Xms64m -Xmx200m -XX:MaxMetaspaceSize=64m -XX:+UseG1GC"
```

## 内存分配建议

在 200MB 总内存限制下:
- **堆内存**: 最多 200MB（-Xmx200m）
- **元空间**: 最多 64MB
- **线程栈**: 默认 1MB/线程 × ~30 线程 ≈ 30MB
- **直接内存/NIO**: ~20MB
- **总估算**: ~200m + 64m + 30m + 20m ≈ 314MB 进程总内存

如需进一步压缩内存，可考虑:
- 减少 `-Xmx` 至 128m
- 使用 `-XX:MaxMetaspaceSize=48m`
- 添加 `-Xss512k` 减少线程栈大小
- 启用 `-XX:+TieredCompilation -XX:TieredStopAtLevel=1` 减少 JIT 编译内存
