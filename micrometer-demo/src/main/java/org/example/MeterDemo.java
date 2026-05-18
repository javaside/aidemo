package org.example;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Micrometer 核心指标类型 — Timer、Counter、LongTaskTimer 直接使用（无需 Observation）。
 *
 * <h3>三种指标</h3>
 * <pre>
 * Timer         → 测量<b>短期操作耗时</b>（如 HTTP 请求、DB 查询），自动算 count/mean/max/P99
 * LongTaskTimer → 测量<b>正在运行中的长期任务</b>（如批处理、定时任务），能查当前在跑的任务数
 * Counter       → 只增不减的<b>计数</b>（如请求数、错误数、处理条数）
 * </pre>
 *
 * <h3>什么时候直接用它们，而不是 Observation？</h3>
 * Observation 帮你自动生成 Timer + Counter，但你如果只想记录一个简单的计数
 * 或时长，不需要 Observation 全套生命周期，直接拿 MeterRegistry 创建指标更轻量。
 */
public class MeterDemo {

    static MeterRegistry registry = new SimpleMeterRegistry();

    public static void main(String[] args) {
        timerDemo();
        counterDemo();
        longTaskTimerDemo();
        histogramDemo();
        summary();
    }

    // ========================================================================
    // 1. Timer — 测量短期操作耗时
    // ========================================================================

    /**
     * Timer 记录每次操作的耗时，自动统计：调用次数、平均耗时、最大耗时。
     *
     * <p><b>注意</b>：Timer 在操作<b>完成后</b>才记录。适合毫秒到分钟级别的操作。
     * 不能用于小时级别的任务——那要用 LongTaskTimer。</p>
     */
    static void timerDemo() {
        header("1. Timer — 测量短期操作耗时");

        Timer orderPaymentTimer = Timer.builder("order.payment")
            .description("订单支付耗时")
            .tag("channel", "alipay")
            .register(registry);

        // 模拟 3 次支付，各自耗时不同
        orderPaymentTimer.record(() -> {
            System.out.println("  支付-1 处理中...");
            sleep(80);
        });

        orderPaymentTimer.record(() -> {
            System.out.println("  支付-2 处理中...");
            sleep(150);
        });

        orderPaymentTimer.record(() -> {
            System.out.println("  支付-3 处理中...");
            sleep(50);
        });

        System.out.printf("%n  Timer 统计结果:%n");
        System.out.printf("    count=%d（总次数）%n", orderPaymentTimer.count());
        System.out.printf("    totalTime=%.0fms（总耗时）%n",
            orderPaymentTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
        System.out.printf("    mean=%.0fms（平均耗时）%n",
            orderPaymentTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        System.out.printf("    max=%.0fms（单次最长）%n",
            orderPaymentTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    // ========================================================================
    // 2. Counter — 只增不减的计数
    // ========================================================================

    /**
     * Counter 只有 {@code increment()}，没有 decrement()。适合记录只增不减的东西：
     * 请求次数、错误次数、消息发送量。
     *
     * <p>注意：Counter 不关心耗时，只记录"发生了多少次"。</p>
     */
    static void counterDemo() {
        header("2. Counter — 只增不减的计数");

        Counter orderCounter = Counter.builder("order.created")
            .description("创建订单数量")
            .tag("status", "success")
            .register(registry);

        Counter errorCounter = Counter.builder("order.created")
            .description("创建订单数量")
            .tag("status", "fail")
            .register(registry);

        // 模拟收到 5 个订单，其中 1 个失败
        String[] results = {"success", "success", "fail", "success", "success"};
        for (String r : results) {
            if ("success".equals(r)) {
                orderCounter.increment();
                System.out.println("  订单创建成功 → 成功计数 +1");
            } else {
                errorCounter.increment();
                System.out.println("  订单创建失败 → 失败计数 +1");
            }
            sleep(30);
        }

        System.out.printf("%n  Counter 统计结果:%n");
        System.out.printf("    成功: %.0f 次%n", orderCounter.count());
        System.out.printf("    失败: %.0f 次%n", errorCounter.count());
        System.out.printf("    成功率: %.0f%%%n",
            orderCounter.count() / (orderCounter.count() + errorCounter.count()) * 100);
    }

    // ========================================================================
    // 3. LongTaskTimer — 测量正在运行中的长期任务
    // ========================================================================

    /**
     * LongTaskTimer 与 Timer 最关键的区别：
     * <ul>
     *   <li><b>Timer</b>: 操作<b>结束后</b>记录耗时 ← 运行时查不到</li>
     *   <li><b>LongTaskTimer</b>: <b>运行时</b>就能查到"当前有几个任务在跑"和"已跑多久"</li>
     * </ul>
     *
     * <p>典型场景：批处理作业、定时任务、长时间数据同步。
     * 运维在 Prometheus 上能看到当前有多少任务在执行、跑了多久还没结束。</p>
     */
    static void longTaskTimerDemo() {
        header("3. LongTaskTimer — 测量正在运行的长期任务");

        LongTaskTimer batchTimer = LongTaskTimer.builder("data.sync")
            .description("数据同步任务耗时")
            .tag("job", "daily-export")
            .register(registry);

        System.out.println("  启动 2 个数据同步任务...\n");

        // 启动任务 1
        Thread t1 = new Thread(() -> {
            LongTaskTimer.Sample sample = batchTimer.start();
            System.out.println("  [任务1] 开始执行，预计 600ms");
            sleep(600);
            System.out.println("  [任务1] 完成");
            sample.stop();
        }, "sync-task-1");
        t1.start();

        // 启动任务 2（晚 100ms 启动）
        Thread t2 = new Thread(() -> {
            sleep(100);
            LongTaskTimer.Sample sample = batchTimer.start();
            System.out.println("  [任务2] 开始执行，预计 400ms");
            sleep(400);
            System.out.println("  [任务2] 完成");
            sample.stop();
        }, "sync-task-2");
        t2.start();

        // 在任务运行期间，轮询查看 LongTaskTimer 的当前状态
        for (int i = 0; i < 3; i++) {
            sleep(250);
            System.out.printf("  [轮询 %d] 当前正在运行的任务: %d 个, 已运行最长时间: %.0fms%n",
                i + 1,
                batchTimer.activeTasks(),
                batchTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
        }

        try { t1.join(); t2.join(); } catch (InterruptedException e) {}

        System.out.printf("%n  LongTaskTimer 统计结果:%n");
        System.out.printf("    activeTasks=%d（所有任务完成后应为 0）%n",
            batchTimer.activeTasks());
        System.out.printf("    duration=%.0fms（当前活跃任务总耗时，结束后为 0）%n",
            batchTimer.duration(java.util.concurrent.TimeUnit.MILLISECONDS));
        System.out.printf("    max=%.0fms（最长单次耗时）%n",
            batchTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    // ========================================================================
    // 4. HistogramSnapshot — P99 分位数 + 直方图桶
    // ========================================================================

    /**
     * Timer 直接提供 {@code count()/mean()/max()}，但想拿到 <b>P99/P95</b>
     * 或 <b>耗时分布直方图</b>，就要通过 {@code takeSnapshot()} 获取
     * {@link io.micrometer.core.instrument.distribution.HistogramSnapshot}。
     *
     * <p>流程：Timer 录制 → takeSnapshot() → 拿到 percentileValues + histogramCounts。</p>
     */
    static void histogramDemo() {
        header("4. HistogramSnapshot — P99 分位数 + 直方图桶");

        // 创建 Timer 时启用 P50/P75/P95/P99 分位数 +  SLA 直方图桶
        Timer apiTimer = Timer.builder("api.latency")
            .description("接口延迟")
            .publishPercentiles(0.5, 0.75, 0.95, 0.99)  // ← 开启分位数
            .sla(
                java.time.Duration.ofMillis(50),
                java.time.Duration.ofMillis(100),
                java.time.Duration.ofMillis(150),
                java.time.Duration.ofMillis(200))            // ← 显式设定直方图桶边界
            .register(registry);

        // 模拟 100 次 API 调用，正态分布 ≈ N(100ms, 30ms)
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < 100; i++) {
            double latency = Math.max(10, 100 + rng.nextGaussian() * 30);
            apiTimer.record((long) latency, java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        // 直接方法（不需要 snapshot）
        System.out.printf("  直接方法:%n");
        System.out.printf("    count=%d, mean=%.0fms, max=%.0fms%n",
            apiTimer.count(),
            apiTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
            apiTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));

        // takeSnapshot() → 拿到百分位 + 直方图桶
        var snapshot = apiTimer.takeSnapshot();

        System.out.printf("%n  takeSnapshot() → 百分位:%n");
        for (var vp : snapshot.percentileValues()) {
            System.out.printf("    P%d = %.0fms%n",
                (int) (vp.percentile() * 100),
                vp.value(java.util.concurrent.TimeUnit.MILLISECONDS));
        }

        System.out.printf("%n  takeSnapshot() → 直方图桶分布:%n");
        for (var cb : snapshot.histogramCounts()) {
            System.out.printf("    ≤ %6.0fms : %3.0f 次%n",
                cb.bucket(java.util.concurrent.TimeUnit.MILLISECONDS),
                cb.count());
        }

        System.out.println();
        System.out.println("  什么时候用 takeSnapshot()？");
        System.out.println("    mean/max        → Timer 直接给，不用 snapshot");
        System.out.println("    P99/P95 分位数  → 需要 takeSnapshot().percentileValues()");
        System.out.println("    耗时分布直方图   → 需要 takeSnapshot().histogramCounts()");
    }

    // ========================================================================
    // 总结
    // ========================================================================

    static void summary() {
        header("总结：Timer vs LongTaskTimer vs Counter");

        System.out.println("  类型              何时用                       能查到什么");
        System.out.println("  ────────────────  ──────────────────────────  ──────────────────────");
        System.out.println("  Timer             短期操作（ms~分钟级）        count/mean/max/P99");
        System.out.println("  LongTaskTimer     长期任务（分钟~小时级）      activeTasks/当前时长");
        System.out.println("  Counter           只增不减的事件计数            count");
        System.out.println();
        System.out.println("  HistogramSnapshot → takeSnapshot() 拿 P99/P95 分位数 + 直方图桶分布");
        System.out.println();
        System.out.println("  Observation 自动生成 Timer + Counter，高频场景直接用 Observation。");
        System.out.println("  只需简单计数或手工计时时，直接用 MeterRegistry 创建更轻量。");
    }

    // ========================================================================
    // 辅助
    // ========================================================================

    static void header(String title) {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("  " + title);
        System.out.println("─".repeat(60) + "\n");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
