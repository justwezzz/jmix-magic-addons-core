package org.magic.jmix.addons.core.component;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.grid.Grid;
import org.magic.jmix.addons.core.MagicCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.ServletContext;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 游标分页 Grid 安装入口。
 * <p>
 * 通过 {@link #install(Grid, CursorPageFetcher)} 静态方法激活，一行代码完成游标分页懒加载配置。
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * CursorPagedDataProvider<Item> provider = CursorLazyGrid.install(grid, cursor -> service.search(cursor))
 *         .pageSize(50)
 *         .apply();
 * }</pre>
 *
 * @see CursorPagedDataProvider
 */
public final class CursorLazyGrid {

    private static final Logger log = LoggerFactory.getLogger(CursorLazyGrid.class);

    // 估计增量 = pageSize：让 itemCount 事件粒度与每页加载粒度对齐，
    // 避免事件按 2×pageSize 跳导致标题/计数漏掉中间页（undefined size 模式 assumedSize 按此增量跳）
    private static final int ITEM_COUNT_ESTIMATE_INCREASE_MULTIPLIER = 1;
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;

    private CursorLazyGrid() {
        // 工具类，禁止实例化
    }

    /**
     * 安装游标分页懒加载数据提供器。
     *
     * @param grid    Grid 组件
     * @param fetcher 游标分页加载函数
     * @param <T>     数据项类型
     * @return Builder 用于链式配置
     */
    public static <T> Builder<T> install(Grid<T> grid, CursorPageFetcher<T> fetcher) {
        return new Builder<>(grid, fetcher);
    }

    /**
     * 配置构建器。
     *
     * @param <T> 数据项类型
     */
    public static class Builder<T> {
        private final Grid<T> grid;
        private final CursorPageFetcher<T> fetcher;
        private final ApplicationContext ctx;
        private int pageSize;
        private ExecutorService executor;

        private Builder(Grid<T> grid, CursorPageFetcher<T> fetcher) {
            this.grid = Objects.requireNonNull(grid, "grid must not be null");
            this.fetcher = Objects.requireNonNull(fetcher, "fetcher must not be null");
            this.ctx = getApplicationContext();
            this.pageSize = ctx.getBean(MagicCoreProperties.class).getLazyPageSize();
        }

        /** 每页大小，默认从 MagicCoreProperties 读取 */
        public Builder<T> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /** 自定义执行器。传入后由调用方负责生命周期 */
        public Builder<T> executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * 应用游标分页懒加载配置。
         *
         * @return 创建的 CursorPagedDataProvider 实例
         */
        public CursorPagedDataProvider<T> apply() {
            CursorPagedDataProvider<T> provider = new CursorPagedDataProvider<>(fetcher);
            ExecutorService resolvedExecutor = executor != null ? executor : createExecutor();
            boolean ownExecutor = executor == null;

            grid.setPageSize(pageSize);
            grid.setItems(provider);
            grid.getDataCommunicator().setItemCountEstimate(pageSize);
            grid.getDataCommunicator().setItemCountEstimateIncrease(
                    pageSize * ITEM_COUNT_ESTIMATE_INCREASE_MULTIPLIER);
            grid.getDataCommunicator().enablePushUpdates(resolvedExecutor);

            if (ownExecutor) {
                grid.addDetachListener(event -> shutdownExecutor(event, resolvedExecutor));
            }

            log.debug("CursorLazyGrid installed: pageSize={}, ownExecutor={}", pageSize, ownExecutor);
            return provider;
        }

        private ExecutorService createExecutor() {
            AtomicInteger counter = new AtomicInteger();
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable,
                        "cursor-lazy-grid-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            return Executors.newCachedThreadPool(threadFactory);
        }

        private void shutdownExecutor(DetachEvent event, ExecutorService executor) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.debug("CursorLazyGrid executor shutdown: grid={}", event.getSource());
        }

        private static ApplicationContext getApplicationContext() {
            ServletContext servletContext = com.vaadin.flow.server.VaadinServlet.getCurrent().getServletContext();
            return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        }
    }
}
