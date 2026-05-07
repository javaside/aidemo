package org.example;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.ObservationConvention;

/**
 * Micrometer Observation 新手入门。
 *
 * <h3>Observation 是什么？</h3>
 * 一句话：帮你<b>测量一段代码的执行时间和执行次数</b>的工具。
 *
 * <h3>核心组件</h3>
 * <pre>
 *                    ┌─────────────────────┐
 *                    │  ObservationRegistry │  ← 注册中心，所有组件的"大本营"
 *                    └────────┬────────────┘
 *                             │ 持有
 *              ┌──────────────┼──────────────┐
 *              ▼              ▼              ▼
 *       ObservationHandler  Filter      Predicate
 *       (响应生命周期)     (修改标签)   (条件跳过)
 *              │
 *              │ 读写
 *              ▼
 *       Observation.Context   ←── Convention (命名约定)
 *       (携带数据)
 *   </pre>
 *
 * <p>运行 main() 即可看到每个组件的效果。</p>
 */
public class ObservationDemo {

    public static void main(String[] args) {
        intro();           // 0. 快速入门（含 Handler 生命周期演示）
        context();         // 1. Context: 携带数据 + 自定义Context
        filterAndPredicate(); // 2. Filter + Predicate: 全局控制
        convention();      // 3. Convention: 命名与标签约定
        manual();          // 4. 手动生命周期: Event + 异常
    }

    // ========================================================================
    // 0. 快速入门 — 3 步得到指标
    // ========================================================================

    /**
     * 场景：你写了一个"查询用户"的方法，想知道它的调用次数和耗时。
     *
     * <pre>
     * 3 个步骤：
     *   ① 创建 MeterRegistry + ObservationRegistry（一次性启动代码）
     *   ② 用 observe() 包裹业务代码
     *   ③ 查看自动生成的指标
     * </pre>
     *
     * <p><b>lowCardinalityKeyValue vs highCardinalityKeyValue</b>
     * <ul>
     *   <li>low: userId=1001/1002，值有限 → <b>成为指标标签</b>，用来按维度查指标</li>
     *   <li>high: traceId=每次唯一 → <b>不成为指标标签</b>（防止 Prometheus 爆炸），
     *       但 Context 中有，Handler/日志系统可以读取，用来关联日志和链路</li>
     * </ul>
     *
     * <p><b>Handler 生命周期</b>：onStart → onStop/onError。
     * 一次埋点可以注册多个 Handler 各司其职（指标、日志、链路），业务代码不用改。</p>
     */
    static void intro() {
        header("0. 快速入门（含 Handler 生命周期）");

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry obsRegistry = ObservationRegistry.create();
        obsRegistry.observationConfig()
            .observationHandler(new DefaultMeterObservationHandler(meterRegistry))
            // 加一个 Handler 展示如何读取 highCardinality 的 traceId
            .observationHandler(new ObservationHandler<Observation.Context>() {
                @Override public boolean supportsContext(Observation.Context ctx) { return true; }
                @Override public void onStart(Observation.Context ctx) {
                    // highCardinality 的值在 Context 中，不在指标标签里
                    String tid = ctx.getHighCardinalityKeyValue("traceId") != null
                        ? ctx.getHighCardinalityKeyValue("traceId").getValue() : "-";
                    System.out.printf("    [日志 traceId=%s] 查询开始%n", tid);
                }
                @Override public void onStop(Observation.Context ctx) {
                    String tid = ctx.getHighCardinalityKeyValue("traceId").getValue();
                    System.out.printf("    [日志 traceId=%s] 查询结束%n", tid);
                }
                @Override public void onError(Observation.Context ctx) {
                    System.out.println("    [日志] 异常: " + ctx.getError().getMessage());
                }
            });

        //   ② 包裹业务代码 — 跑 3 次，同时设 low 和 high 标签
        Observation.createNotStarted("user.query", obsRegistry)
            .lowCardinalityKeyValue("userId", "1001")
            .highCardinalityKeyValue("traceId", "trace-001")
            .observe(() -> {
                System.out.println("    查询 userId=1001... 查到: 张三");
                sleep(80);
            });

        Observation.createNotStarted("user.query", obsRegistry)
            .lowCardinalityKeyValue("userId", "1001")
            .highCardinalityKeyValue("traceId", "trace-002")
            .observe(() -> {
                System.out.println("    查询 userId=1001... 查到: 张三");
                sleep(50);
            });

        Observation.createNotStarted("user.query", obsRegistry)
            .lowCardinalityKeyValue("userId", "1002")
            .highCardinalityKeyValue("traceId", "trace-003")
            .observe(() -> {
                System.out.println("    查询 userId=1002... 查到: 李四");
                sleep(30);
            });

        //   ③ 查看指标
        System.out.println("\n  自动得到的指标（只有 low 标签作为维度）:");
        for (io.micrometer.core.instrument.Meter m : meterRegistry.getMeters()) {
            if (m instanceof Timer t) {
                String userId = t.getId().getTag("userId");
                System.out.printf("    user.query{userId=%s}: 次数=%d, 平均=%.0fms%n",
                    userId, t.count(),
                    t.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
            }
        }
        System.out.println("\n  总结:");
        System.out.println("    lowCardinality  →  userId 出现在指标标签中，可按 userId 分别统计");
        System.out.println("    highCardinality →  traceId 不出现在指标中，但 Handler 从 Context 读到了");
        System.out.println("    真正用途：出问题时用 traceId=xxx 去 ELK/链路系统搜索，看到完整调用链");
        System.out.println("    核心公式: createNotStarted → lowCardinalityKeyValue → observe");
    }

    // ========================================================================
    // 1. Observation.Context — 携带数据
    // ========================================================================

    /**
     * <b>Observation.Context</b> 是一个"数据袋子"，贯穿 start → stop 整个过程。
     * Handler 在 onStart/onStop 中可以从 Context 取出数据，无需用 ThreadLocal。
     *
     * <p>两种标签：</p>
     * <ul>
     *   <li><b>低基数 (lowCardinality)</b>: 值是有界的（如 userId=1001），会成为指标标签</li>
     *   <li><b>高基数 (highCardinality)</b>: 值是无界的（如 traceId=abc-xyz），<b>不会</b>成为指标标签</li>
     * </ul>
     *
     * <p><b>自定义 Context</b>：继承 Observation.Context，添加类型安全的业务字段。
     * Handler 通过 instanceof 匹配，直接调用 getter 取值。</p>
     */
    static void context() {
        header("1. Observation.Context — 携带数据");

        ObservationRegistry obsRegistry = ObservationRegistry.create();
        obsRegistry.observationConfig()
            .observationHandler(new ObservationHandler<UserQueryContext>() {
                @Override public boolean supportsContext(Observation.Context ctx) {
                    return ctx instanceof UserQueryContext; // 只处理用户查询
                }
                @Override public void onStart(UserQueryContext ctx) {
                    System.out.printf("  [Handler] 查询开始: userId=%s, fromCache=%s%n",
                        ctx.getUserId(), ctx.isFromCache());
                }
                @Override public void onStop(UserQueryContext ctx) {
                    System.out.printf("  [Handler] 查询结束: userId=%s%n", ctx.getUserId());
                }
            });

        // 使用自定义 Context 执行观测
        UserQueryContext ctx = new UserQueryContext("1001", true);
        Observation.createNotStarted("user.query", () -> ctx, obsRegistry)
            .lowCardinalityKeyValue("userId", ctx.getUserId())   // 低基数 → 指标标签
            .highCardinalityKeyValue("traceId", "abc-xyz-123")    // 高基数 → 不做指标标签
            .observe(() -> {
                System.out.println("  查询数据库...");
                sleep(30);
            });

        System.out.println("\n  → Context 把 userId/fromCache 传给了 Handler，不用 ThreadLocal");
        System.out.println("  → 低基数标签(userId)会成为指标维度，高基数标签(traceId)不会");
    }

    /** 自定义 Context：把业务字段建模为类型安全的属性 */
    static class UserQueryContext extends Observation.Context {
        private final String userId;
        private final boolean fromCache;
        UserQueryContext(String userId, boolean fromCache) {
            this.userId = userId;
            this.fromCache = fromCache;
        }
        String getUserId() { return userId; }
        boolean isFromCache() { return fromCache; }
    }

    // ========================================================================
    // 2. ObservationFilter + ObservationPredicate — 全局控制
    // ========================================================================

    /**
     * <b>ObservationFilter 什么作用？</b>
     * 在 Observation.stop() 时、Handler.onStop() 之前，修改 Context。
     * 一处配置，所有观测自动注入全局标签，不用每个调用点手动加。
     *
     * <p><b>ObservationPredicate 什么作用？</b>
     * 在 Observation 创建时判断是否<b>整个跳过</b>。
     * 返回 false → Observation 变成空操作 → 所有 Handler 都不触发。
     * 不只是"不生成指标"，而是连自定义的日志 Handler 也一起跳过。
     *
     * <p><b>什么时候用 Predicate？</b>
     * 健康检查、探测请求这类"非业务"调用，你不想让它们：
     * <ul>
     *   <li>出现在指标里（否则健康检查的 10ms 延迟会拉低业务接口的平均值）</li>
     *   <li>出现在日志里（否则日志被健康检查刷屏）</li>
     *   <li>出现在链路追踪里（否则满屏都是 /health 的 Span）</li>
     * </ul>
     * Predicate 一次性全部拦截。</p>
     */
    static void filterAndPredicate() {
        header("2. ObservationFilter + ObservationPredicate");

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry obsRegistry = ObservationRegistry.create();
        obsRegistry.observationConfig()
            .observationHandler(new DefaultMeterObservationHandler(meterRegistry))
            // 加一个日志 Handler，用来观察 Predicate 是否连它一起跳过
            .observationHandler(new ObservationHandler<Observation.Context>() {
                @Override public boolean supportsContext(Observation.Context ctx) { return true; }
                @Override public void onStart(Observation.Context ctx) {
                    System.out.println("    [Handler] onStart: " + ctx.getName());
                }
            })
            // Filter: 一处配置，所有观测自动带上 env
            .observationFilter(ctx -> ctx
                .addLowCardinalityKeyValue(KeyValue.of("env", "prod")))
            // Predicate: health.check 整个跳过
            .observationPredicate((name, ctx) -> {
                if ("health.check".equals(name)) return false;
                return true;
            });

        System.out.println("  --- user.query（正常，Handler 会触发，指标会生成）---");
        Observation.createNotStarted("user.query", obsRegistry).observe(() -> sleep(20));

        System.out.println("\n  --- health.check（Predicate 返回 false，整个跳过）---");
        Observation.createNotStarted("health.check", obsRegistry)
            .observe(() -> System.out.println("    业务代码照常执行（但观测被跳过了）"));

        System.out.println("\n  最终指标 — health.check 完全不在其中：");
        meterRegistry.getMeters().forEach(m ->
            System.out.println("    " + m.getId().getName() + " " + m.getId().getTags()));

        System.out.println("\n  → 注意：health.check 连 [Handler] onStart 都没输出");
        System.out.println("  → Predicate 返回 false 后，所有 Handler 都不会被触发");
        System.out.println("  → 业务代码虽然执行了，但观测数据（指标/日志/链路）全部丢弃");
    }

    // ========================================================================
    // 3. ObservationConvention — 命名与标签约定
    // ========================================================================

    /**
     * <b>ObservationConvention</b>：把"指标名称"和"标签"从业务代码中抽离。
     *
     * <p>痛点：如果你的支付方法写了 name="payment.execute"，
     * 运营团队想改成 name="payment.order.pay"，你就得改代码。
     *
     * <p>Convention 的解法：业务代码只声明"这是一次支付操作"，
     * 名称和标签由 Convention 决定。想改名？换一个 Convention 实现就行，业务代码不动。</p>
     *
     * <p>优先级（高→低）：<b>直接传入的 Convention > GlobalConvention > 默认 Convention</b></p>
     */
    static void convention() {
        header("3. ObservationConvention — 不改代码切换指标命名");

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry obsRegistry = ObservationRegistry.create();
        obsRegistry.observationConfig()
            .observationHandler(new DefaultMeterObservationHandler(meterRegistry));

        // 使用默认约定
        PaymentContext ctx1 = new PaymentContext("alipay", "ORDER-001");
        Observation.createNotStarted(new DefaultPayConvention(), () -> ctx1, obsRegistry)
            .observe(() -> sleep(20));
        System.out.println("  默认约定 → 指标名: payment.execute");

        // 使用自定义约定（业务代码完全一样）
        PaymentContext ctx2 = new PaymentContext("alipay", "ORDER-002");
        Observation.createNotStarted(new CustomPayConvention(), () -> ctx2, obsRegistry)
            .observe(() -> sleep(20));
        System.out.println("  自定义约定 → 指标名: payment.order.pay");
        System.out.println("  → 业务代码一样，换 Convention 就换了指标名和标签");
    }

    static class PaymentContext extends Observation.Context {
        private final String method;
        private final String orderId;
        PaymentContext(String method, String orderId) {
            this.method = method;
            this.orderId = orderId;
        }
        String getMethod() { return method; }
        String getOrderId() { return orderId; }
    }

    /** 默认约定 */
    static class DefaultPayConvention implements ObservationConvention<PaymentContext> {
        @Override public boolean supportsContext(Observation.Context ctx) { return ctx instanceof PaymentContext; }
        @Override public String getName() { return "payment.execute"; }
        @Override public KeyValues getLowCardinalityKeyValues(PaymentContext ctx) {
            return KeyValues.of(KeyValue.of("method", ctx.getMethod()));
        }
        @Override public KeyValues getHighCardinalityKeyValues(PaymentContext ctx) {
            return KeyValues.of(KeyValue.of("orderId", ctx.getOrderId()));
        }
    }

    /** 自定义约定 — 覆盖名称和标签 */
    static class CustomPayConvention extends DefaultPayConvention {
        @Override public String getName() { return "payment.order.pay"; }
        @Override public KeyValues getLowCardinalityKeyValues(PaymentContext ctx) {
            return super.getLowCardinalityKeyValues(ctx)
                .and(KeyValue.of("channel", "app"));
        }
    }

    // ========================================================================
    // 4. 手动生命周期 — Event + 异常
    // ========================================================================

    /**
     * {@code observe(() -> {...})} 适合 90% 的场景。
     * 少数情况需要手动控制，比如要在执行过程中记录自定义 <b>Event</b>。
     *
     * <p>手动模式的完整写法：</p>
     * <pre>
     *   Observation obs = Observation.start(name, registry);
     *   try (Scope scope = obs.openScope()) {
     *       obs.event(Event.of("事件名"));  // ← 可在 Handler.onEvent 中收到
     *       业务逻辑...
     *   } catch (Exception e) {
     *       obs.error(e);                   // ← 触发 Handler.onError + 自动 error 标签
     *       throw e;
     *   } finally {
     *       obs.stop();                     // ← 必须 stop
     *   }
     * </pre>
     */
    static void manual() {
        header("4. 手动生命周期 — Event + 异常处理");

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry obsRegistry = ObservationRegistry.create();
        obsRegistry.observationConfig()
            .observationHandler(new DefaultMeterObservationHandler(meterRegistry));

        // 正常流程：记录自定义事件
        System.out.println("  --- 正常流程（带事件）---");
        Observation obs1 = Observation.start("payment.manual", obsRegistry);
        try (Observation.Scope scope = obs1.openScope()) {
            obs1.event(Event.of("risk.check.pass"));  // 风控通过 → 自动生成 Counter
            sleep(30);
            obs1.event(Event.of("coupon.apply"));      // 优惠券 → 自动生成 Counter
            sleep(20);
        } finally {
            obs1.stop();
        }

        // 异常流程：error() 记录异常
        System.out.println("\n  --- 异常流程 ---");
        Observation obs2 = Observation.start("payment.manual", obsRegistry);
        try (Observation.Scope scope = obs2.openScope()) {
            sleep(10);
            throw new RuntimeException("余额不足");
        } catch (RuntimeException e) {
            obs2.error(e);   // ← 触发 error 标签 + Handler.onError
        } finally {
            obs2.stop();
        }

        System.out.println("\n  产生的指标：");
        meterRegistry.getMeters().forEach(m ->
            System.out.println("    " + m.getId().getType() + " "
                + m.getId().getName() + " " + m.getId().getTags()));
        System.out.println("\n  → 2 个 Event → 2 个 Counter。异常 → Timer 带 error=RuntimeException 标签");
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
