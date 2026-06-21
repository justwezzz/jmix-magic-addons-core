package org.magic.jmix.addons.core.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import io.jmix.flowui.component.grid.TreeDataGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * TreeGrid 滚动定位工具类。
 * <p>
 * 提供静态方法滚动 TreeGrid 到指定节点位置。
 * 使用 Grid 内部的 _scrollToFlatIndex 方法实现虚拟滚动定位。
 *
 * @param <T> 节点类型
 */
public final class TreeGridScrollHelper {

    private static final Logger log = LoggerFactory.getLogger(TreeGridScrollHelper.class);

    private static final int SCROLL_DELAY_MS = 50;
    private static final int CALLBACK_DELAY_MS = 100;

    private TreeGridScrollHelper() {
        // 工具类，禁止实例化
    }

    /**
     * 滚动 TreeGrid 到指定节点位置。
     *
     * @param grid       TreeDataGrid 组件
     * @param treeData   TreeData 数据
     * @param targetNode 目标节点
     * @param <T>        节点类型
     * @return 计算的扁平索引（可用于调试），空表示节点未找到
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode) {
        return scrollToNode(grid, treeData, targetNode, null, null);
    }

    /**
     * 滚动 TreeGrid 到指定节点位置（带回调）。
     *
     * @param grid       TreeDataGrid 组件
     * @param treeData   TreeData 数据
     * @param targetNode 目标节点
     * @param callback   滚动完成回调（延迟 100ms 触发），可为 null
     * @param <T>        节点类型
     * @return 计算的扁平索引，空表示节点未找到
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode,
            Consumer<Integer> callback) {
        return scrollToNode(grid, treeData, targetNode, null, callback);
    }

    /**
     * 滚动 TreeGrid 到指定节点位置（完整参数）。
     *
     * @param grid       TreeDataGrid 组件
     * @param treeData   TreeData 数据
     * @param targetNode 目标节点
     * @param filter     节点过滤器，返回 true 表示该节点参与索引计算，可为 null（不过滤）
     * @param callback   滚动完成回调（延迟 100ms 触发），可为 null
     * @param <T>        节点类型
     * @return 计算的扁平索引，空表示节点未找到
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode,
            Predicate<T> filter,
            Consumer<Integer> callback) {

        int idx = calculateFlatIndex(grid, treeData, targetNode, filter);

        if (idx < 0) {
            log.debug("scrollToNode: target node not found in visible tree");
            return Optional.empty();
        }

        log.debug("scrollToNode: calculated flat index = {}", idx);

        final int finalIdx = idx;
        grid.getUI().ifPresent(ui -> ui.getPage().executeJs(
                "setTimeout(function() {" +
                "  var grid = $0;" +
                "  if (grid && grid._scrollToFlatIndex) {" +
                "    grid._scrollToFlatIndex(" + finalIdx + ");" +
                "  }" +
                "}, " + SCROLL_DELAY_MS + ");",
                grid.getElement()
        ));

        if (callback != null) {
            int totalDelay = SCROLL_DELAY_MS + CALLBACK_DELAY_MS;
            grid.getUI().ifPresent(ui -> ui.getPage().executeJs(
                    "setTimeout(function() { }, " + totalDelay + ");"
            ).then(success -> callback.accept(finalIdx)));
        }

        return Optional.of(idx);
    }

    /**
     * 计算节点在当前展开状态下的扁平索引。
     *
     * @param grid       TreeDataGrid 组件
     * @param treeData   TreeData 数据
     * @param targetNode 目标节点
     * @param filter     节点过滤器，可为 null
     * @return 扁平索引，-1 表示未找到
     */
    private static <T> int calculateFlatIndex(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode,
            Predicate<T> filter) {

        if (treeData == null) {
            return -1;
        }

        int index = 0;
        List<T> roots = treeData.getRootItems();

        for (T root : roots) {
            int result = traverseForIndex(grid, treeData, root, targetNode, index, filter);
            if (result >= 0) {
                return result;
            }
            index = -result;
        }

        return -1;
    }

    /**
     * 递归遍历树，计算目标节点的扁平索引。
     *
     * @param grid       TreeDataGrid 组件
     * @param treeData   TreeData 数据
     * @param node       当前节点
     * @param targetNode 目标节点
     * @param startIndex 当前起始索引
     * @param filter     节点过滤器，可为 null
     * @return 找到目标返回索引值；未找到返回负数（已遍历的节点数）
     */
    private static <T> int traverseForIndex(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T node,
            T targetNode,
            int startIndex,
            Predicate<T> filter) {

        int currentIndex = startIndex;

        // 检查是否过滤该节点
        boolean shouldCount = (filter == null || filter.test(node));

        if (node.equals(targetNode)) {
            // 目标节点如果被过滤，视为未找到
            if (!shouldCount) {
                // 继续遍历返回已计数的节点数（负数）
                return -(currentIndex + 1); // +1 表示这个节点也算过了，但不算入索引
            }
            return currentIndex;
        }

        if (shouldCount) {
            currentIndex++;
        }

        if (grid.isExpanded(node)) {
            List<T> children = treeData.getChildren(node);
            for (T child : children) {
                int result = traverseForIndex(grid, treeData, child, targetNode, currentIndex, filter);
                if (result >= 0) {
                    return result;
                }
                currentIndex = -result;
            }
        }

        return -currentIndex;
    }
}
