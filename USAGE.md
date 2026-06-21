# Jmix Magic Core Addon - 使用指南

本文档详细介绍 `jmix-magic-addons-core` 插件的使用方法。

## 目录

1. [BaseEntity - 实体基类](#baseentity---实体基类)
2. [AppConfig - 应用配置](#appconfig---应用配置)
3. [AppConfigService - 配置服务](#appconfigservice---配置服务)
4. [增强 UI 组件](#增强-ui-组件)
5. [LazyContainerDataGridItems - 懒加载 DataGrid](#lazycontainerdatagriditems---懒加载-datagrid)
6. [CursorLazyGrid - 游标分页懒加载 Grid](#cursorlazygrid---游标分页懒加载-grid)
7. [GenericUniqueConstraintViolationHandler - 唯一约束异常处理](#genericuniqueconstraintviolationhandler---唯一约束异常处理)
8. [TreeGridScrollHelper - TreeGrid 滚动定位](#treegridscrollhelper---treegrid-滚动定位)
9. [NotificationUtil - 统一通知工具](#notificationutil---统一通知工具)
10. [MagicDetailWindowBuilderProcessor - 详情对话框列表回写守卫](#magicdetailwindowbuilderprocessor---详情对话框列表回写守卫)
11. [增强 Action - 列表 CRUD 时序统一](#增强-action---列表-crud-时序统一)
12. [DetailViewCloseCallback - 详情关闭回调契约](#detailviewclosecallback---详情关闭回调契约)
13. [多标签契约（tab 包）- 跨 addon 解耦](#多标签契约tab-包--跨-addon-解耦)
14. [视图基类（view.base 包）](#视图基类viewbase-包)

---

## BaseEntity - 实体基类

统一的实体基类，所有项目实体应继承此类，确保审计功能和 ID 风格一致。

### 提供的字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID | 主键，`@JmixGeneratedValue` |
| `version` | Integer | 版本号，`@Version`，乐观锁 |
| `createTs` | Date | 创建时间，`@CreatedDate` |
| `createdBy` | String | 创建人，`@CreatedBy` |
| `updateTs` | Date | 更新时间，`@LastModifiedDate` |
| `updatedBy` | String | 更新人，`@LastModifiedBy` |

### 使用方式

```java
import org.magic.jmix.addons.core.entity.BaseEntity;

@Entity(name = "magic_User")
@Table(name = "MAGIC_USER")
public class User extends BaseEntity {

    @InstanceName
    @Column(name = "USERNAME", nullable = false, unique = true)
    private String username;

    // getter/setter...
}
```

### 最佳实践

1. **所有实体必须继承 BaseEntity** - 保持项目一致性
2. **使用 Metadata.create() 实例化** - 不使用构造函数
3. **审计字段自动填充** - 无需手动设置

---

## AppConfig - 应用配置

键值对形式的应用配置实体，用于存储系统级配置。

### 实体结构

| 字段 | 类型 | 长度 | 说明 |
|------|------|------|------|
| `configKey` | String | 32 | 配置键，唯一 |
| `configValue` | String | 256 | 配置值 |
| `description` | String | 256 | 描述（可选） |

### 数据库表

```sql
CREATE TABLE MAGIC_APP_CONFIG (
    ID UUID NOT NULL,
    CONFIG_KEY VARCHAR(32) NOT NULL UNIQUE,
    CONFIG_VALUE VARCHAR(256),
    DESCRIPTION VARCHAR(256),
    VERSION INTEGER NOT NULL,
    CREATE_TS TIMESTAMP,
    CREATED_BY VARCHAR(50),
    UPDATE_TS TIMESTAMP,
    UPDATED_BY VARCHAR(50),
    PRIMARY KEY (ID)
);
```

---

## AppConfigService - 配置服务

提供配置的读写操作服务。

### API

```java
public interface AppConfigService {

    /**
     * 获取配置值
     * @param key 配置键
     * @return 配置值，不存在时返回 null
     */
    String getValue(String key);

    /**
     * 设置配置值（存在则更新，不存在则创建）
     * @param key 配置键
     * @param value 配置值
     */
    void setValue(String key, String value);

    /**
     * 设置配置值（带描述）
     */
    void setValue(String key, String value, String description);

    /**
     * 删除配置
     * @param key 配置键
     */
    void delete(String key);

    /**
     * 检查配置是否存在
     * @param key 配置键
     * @return 是否存在
     */
    boolean exists(String key);
}
```

### 使用示例

```java
@Autowired
private AppConfigService appConfigService;

// 读取配置
String systemName = appConfigService.getValue("system.name");
if (systemName == null) {
    systemName = "默认系统名"; // 提供默认值
}

// 写入配置
appConfigService.setValue("system.name", "Magic Platform");

// 写入配置（带描述）
appConfigService.setValue("system.version", "1.0.0", "系统版本号");

// 检查是否存在
if (!appConfigService.exists("system.initialized")) {
    // 初始化逻辑...
    appConfigService.setValue("system.initialized", "true", "系统是否已初始化");
}

// 删除配置
appConfigService.delete("system.temp");
```

### 使用场景

- 系统初始化标记
- 动态配置（无需重启生效）
- 功能开关
- 系统版本信息

---

## 增强 UI 组件

插件提供了两个增强组件，通过组件注册机制**自动替换**标准的 Jmix 组件。

### MagicGenericFilter

替换标准的 `GenericFilter`，修复以下问题：

| 问题 | 解决方案 |
|------|----------|
| `autoApply=false` 时条件共享 | 应用条件时进行深克隆，避免多视图实例共享同一条件对象 |
| 无清空按钮 | 添加清空按钮（橡皮擦图标），一键清除所有过滤条件 |
| 应用条件后分页未重置 | 应用过滤条件时自动重置 `firstResult` 为 0 |

**使用方式**：

XML 中使用标准标签即可，无需任何改动：

```xml
<genericFilter id="genericFilter" dataLoader="entitiesDl" opened="false">
    <!-- 过滤条件配置 -->
</genericFilter>
```

插件通过 `ComponentRegistration.replaceComponent()` 自动将 `<genericFilter>` 映射到 `MagicGenericFilter`。

### MagicSimplePagination

替换标准的 `SimplePagination`，修复数据刷新后页码跳动的问题：

| 问题 | 解决方案 |
|------|----------|
| 数据刷新后重置到第一页 | 在 `onRefreshItems` 中记录 `committedFirstResult`，翻页操作时恢复 |

**使用方式**：

XML 中使用标准标签即可，无需任何改动：

```xml
<simplePagination id="pagination" dataLoader="entitiesDl"/>
```

插件通过 `ComponentRegistration.replaceComponent()` 自动将 `<simplePagination>` 映射到 `MagicSimplePagination`。

---

## LazyContainerDataGridItems - 懒加载 DataGrid

懒加载 DataGrid 数据提供器，包装标准 `CollectionContainer` 实现 DB 级分页。

### 核心特性

- 实现 `ContainerDataUnit`，完全兼容标准 CC 的事件机制和 DetailView 流程
- 新建实体自动加入 `prependBuffer`，显示在列表顶部
- 支持 `genericFilter`、排序、`baseCondition`
- 滚动时按需从 DB 加载数据，CC 只持有当前视口数据

### 使用方式

```java
import org.magic.jmix.addons.core.component.LazyContainerDataGridItems;

@ViewComponent
private DataGrid<User> usersGrid;
@Autowired
private DataManager dataManager;
@ViewComponent
private Span totalCountLabel;

@Subscribe
public void onInit(InitEvent event) {
    LazyContainerDataGridItems.install(usersGrid, dataManager)
        .pageSize(50)
        .totalCountLabel(totalCountLabel)
        .apply();
}
```

### Builder 配置项

| 方法 | 说明 |
|------|------|
| `pageSize(int)` | 每页大小，默认从 `magic.core.lazy-page-size` 读取（默认 50） |
| `totalCountLabel(Span)` | 总数显示标签，显示 "1-30 / 100" 格式 |
| `afterDataChange(Runnable)` | 数据变化后的回调（如滚动加载完成） |
| `baseConditionSupplier(Supplier<Condition>)` | 基础条件提供器，用于主从表等场景过滤从表数据 |

### 配置项

```properties
# 懒加载页大小
magic.core.lazy-page-size=50
```

### 数据流程

1. `fetch()` 从 `prependBuffer` 和 DB 获取数据，DB 数据通过 `DataGenerator` 回调同步到 CC
2. 新建实体触发 CC 的 `ADD_ITEMS` 事件，被捕获到 `prependBuffer` 并从 CC 移除避免重复
3. 编辑触发 CC 的 `SET_ITEM` 事件，更新 `prependBuffer` 中对应实体引用
4. 删除触发 CC 的 `REMOVE_ITEMS` 事件，从 `prependBuffer` 移除对应实体

---

## CursorLazyGrid - 游标分页懒加载 Grid

游标分页懒加载 Grid 组件，封装自定义 `AbstractDataProvider`，把**游标顺序分页**的后端（传入 cursor 返回 `items + nextCursor + hasMore`）适配为 Grid 原生的滚动懒加载。适用于 MinIO 搜索、对象存储列举等**后端本身按游标分页、且无精确总数**的场景。

### 与 LazyContainerDataGridItems 的区别

| 维度 | LazyContainerDataGridItems | CursorLazyGrid |
|------|----------------------------|----------------|
| 数据源 | JPA/DB（DataManager + JPQL offset/count） | 任意游标分页后端（fetcher 回调） |
| 分页模型 | offset 随机访问（可跳页、可回查） | 游标顺序推进（只能前进） |
| 总数 | 精确 count | 无 totalCount（undefined size 模式） |
| 缓存 | CC 视口缓存（滚走即回收、按需重取） | **全量缓存（只增不减，见下）** |
| Grid 类型 | Jmix `DataGrid`（需 dataContainer） | 原生 Vaadin `Grid`（也兼容 DataGrid） |

### 核心特性

- 自定义 `AbstractDataProvider`，游标顺序流 → Grid offset 懒加载
- undefined size 模式：滚动到底自动加载下一页，到底靠 `fetch` 返回不足自动收敛（不依赖 size 估计）
- 异步 `enablePushUpdates`：后端阻塞 IO 不冻结 UI 线程（需宿主启用 `@Push`）
- install/Builder 一行接入，默认 pageSize 读 `magic.core.lazy-page-size`

### 使用方式

```java
import org.magic.jmix.addons.core.component.CursorLazyGrid;
import org.magic.jmix.addons.core.component.CursorPagedDataProvider;
import org.magic.jmix.addons.core.component.CursorPage;
import org.magic.jmix.addons.core.component.CursorPageFetcher;

// 1. 定义游标分页回调：后端按 cursor 返回一页（cursor=null 表示从头开始）
CursorPageFetcher<Item> fetcher = cursor -> {
    SearchResult r = backend.searchPaged(keyword, cursor, 50);  // 你的游标分页后端
    return CursorPage.of(r.getItems(), r.getNextCursor(), r.isHasMore());
};

// 2. 安装到原生 Grid（Jmix DataGrid 也可，向上转型兼容）
CursorPagedDataProvider<Item> provider =
        CursorLazyGrid.install(grid, fetcher)
                .pageSize(50)
                .apply();

// 3. 切换搜索条件时重置（清空缓存、游标归零、重新加载）
provider.reset();
```

### Builder 配置项

| 方法 | 说明 |
|------|------|
| `pageSize(int)` | 每页大小，默认从 `magic.core.lazy-page-size` 读取（默认 50） |
| `executor(ExecutorService)` | 自定义异步执行器；不传则组件自建，Grid detach 时自动 shutdown |

### ⚠️ 缓存机制：全量缓存，大数据场景务必评估

> **这是本组件最需要使用者知道的特性。**

`CursorPagedDataProvider` 内部维护一个**只增不减**的已加载缓存 `loaded`。每次滚动加载新页时把数据追加进该缓存，**永不回收**。

**为什么必须全量缓存**：游标只能顺序前进、不能逆向。当用户向上滚回已浏览过的区域时，Grid 会重新请求该 offset 的数据，但游标无法「倒退」重取——所以必须把已加载的全部保留在缓存里，让逆向请求命中。这与 Vaadin 原生 `BackEndDataProvider`（offset 可随机重取、不缓存）本质不同。

**直接后果**：

- **滚到底 = 全量加载到内存**。用户持续滚动，所有匹配项最终都会累积进 `loaded` 缓存，等价于全量加载（只是分批进入）。
- 不会因为 Grid 视口回收而省内存——Grid 层确实只显示视口附近，但 provider 层保住了全部。

**⚠️ 大数据场景谨慎使用**：

- 适合**结果集可控**的场景（搜索匹配项通常几百到几千条，全缓存无压力）。
- 若后端可能返回**海量结果（数万以上）**，用户滚到底会导致内存持续增长，**请谨慎评估**；必要时自行在 fetcher 内加上限（到 N 条后 `hasMore=false` 停止）。
- 若需要真正的「流式省内存」（只缓存视口附近、滚走即回收），应使用 offset 可随机重取的数据源——即 **`LazyContainerDataGridItems`（JPA/DB）**，而非本组件。

### 适用 / 不适用场景

| 适用 ✅ | 不适用 ❌ |
|---------|-----------|
| MinIO / 对象存储搜索（游标分页、无总数） | JPA/DB 大表分页（用 `LazyContainerDataGridItems`） |
| 结果集可控（几百~几千条）的游标数据 | 结果可能海量的游标数据（无上限时内存风险） |
| 后端只有 cursor API、无 offset/count | 需要精确总数或可排序的场景（游标顺序固定） |

---

## GenericUniqueConstraintViolationHandler - 唯一约束异常处理

自动拦截唯一约束异常并显示友好提示信息。

### 支持的约束定义方式

| 方式 | 支持 | 说明 |
|------|------|------|
| `@Index(name = "IDX_XXX", columnList = "COL", unique = true)` | ✅ | 推荐 |
| `@UniqueConstraint(name = "UK_XXX", columnNames = "COL")` | ✅ | 推荐 |
| `@Column(unique = true)` | ❌ | 不支持，数据库自动生成约束名 |

### 工作原理

1. 启动时扫描所有实体的唯一约束定义
2. 捕获数据库唯一约束异常
3. 解析约束名，查找对应的实体和字段
4. 显示友好的中文提示

### 自定义提示消息

默认提示：`已存在相同{字段名}的{实体名}`

可在项目的 `messages.properties` 中自定义：

```properties
# 自定义特定约束的提示
databaseUniqueConstraintViolation.IDX_USER_USERNAME=用户名已存在，请使用其他用户名

# 修改默认模板
org.magic.jmix.addons.core/uniqueConstraintViolation.default=该{1}的{0}已存在

# 多字段连接符
org.magic.jmix.addons.core/fieldConnector=和
```

### 正确定义唯一约束

```java
@Table(name = "MAGIC_USER", indexes = {
    @Index(name = "IDX_USER_USERNAME", columnList = "USERNAME", unique = true),
    @Index(name = "IDX_USER_EMAIL", columnList = "EMAIL", unique = true)
})
public class User extends BaseEntity {
    // ...
}
```

### 外键约束违规处理（GenericForeignKeyViolationHandler）

与唯一约束处理器配套，`GenericForeignKeyViolationHandler`（`@Component("magic_GenericForeignKeyViolationHandler")`）拦截数据库**外键约束违规**异常（删除/更新被引用记录时触发），解析外键约束名并显示友好提示。

- **消息前缀**：`databaseForeignKeyViolation.`，默认模板 key `databaseForeignKeyViolation.default`
- **约束名正则**：默认匹配中英文外键约束异常文本；可用配置 `magic.db.foreignKeyViolationPattern` 自定义
- **自定义提示**：在项目 messages 中按约束名覆盖，如 `databaseForeignKeyViolation.FK_ORDER_CUSTOMER=该订单被客户引用，无法删除`

### 约束元数据扫描（ConstraintMetadataScanner / ConstraintInfo）

`ConstraintMetadataScanner`（`@Component("magic_ConstraintMetadataScanner")`）在应用启动时（`ContextRefreshedEvent`）扫描所有实体的 `@Table` / `@UniqueConstraint` 注解，构建「约束名 → 实体/字段」的映射缓存（`ConstraintInfo`），供上述两个约束处理器在运行时按数据库返回的约束名定位实体与字段。

---

## TreeGridScrollHelper - TreeGrid 滚动定位

TreeGrid 滚动定位工具类，用于在 TreeGrid 中滚动到指定节点位置。

### 背景

Vaadin TreeGrid 使用虚拟滚动机制，直接设置 `scrollTop` 无法正确触发虚拟滚动更新。`TreeGridScrollHelper` 封装了正确的滚动方法，使用 Grid 内部的 `_scrollToFlatIndex` 方法实现滚动。

### API

```java
public class TreeGridScrollHelper {

    /**
     * 滚动 TreeGrid 到指定节点位置（简单场景）
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode);

    /**
     * 滚动 TreeGrid 到指定节点位置（带回调）
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode,
            Consumer<Integer> callback);

    /**
     * 滚动 TreeGrid 到指定节点位置（完整参数）
     *
     * @param filter 过滤器，返回 true 表示该节点参与索引计算，
     *               可为 null（不过滤）。用于过滤占位符等不需要的节点
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode,
            Predicate<T> filter,
            Consumer<Integer> callback);
}
```

### 使用示例

#### 简单场景（无占位符）

```java
Optional<Integer> index = TreeGridScrollHelper.scrollToNode(
    grid,
    treeData,
    targetNode
);

if (index.isEmpty()) {
    showNotification("节点未找到");
}
```

#### 懒加载场景（过滤占位符）

```java
TreeGridScrollHelper.scrollToNode(
    grid,
    treeData,
    targetNode,
    node -> !node.getPath().endsWith(".placeholder"),  // 过滤占位符
    null
);
```

#### 带回调

```java
TreeGridScrollHelper.scrollToNode(
    grid,
    treeData,
    targetNode,
    idx -> {
        // 滚动完成后执行
        showNotification("已定位到节点，索引: " + idx);
    }
);
```

### 前置条件

调用方需要确保：

1. **目标节点在展开树中可见**：如果目标节点的父节点未展开，需要先展开父节点
2. **数据不包含占位符**：如果使用懒加载占位符模式，需要确保 TreeData 中不包含占位符节点

### 实现原理

1. 遍历 TreeData 计算目标节点在当前展开状态下的扁平索引
2. 使用 JavaScript 调用 Grid 内部的 `_scrollToFlatIndex` 方法
3. 延迟 50ms 执行 JS，确保 Grid 内部状态同步完成
4. 回调在滚动后延迟 100ms 触发

### 注意事项

- `_scrollToFlatIndex` 是 Vaadin Grid 的内部方法，未来版本可能变化
- 延迟时间（50ms/100ms）为经验值，在极端情况下可能需要调整
- 方法返回的索引可用于调试日志

---

## NotificationUtil - 统一通知工具

统一通知工具类，按类型（success / error / warning / info）显示正文与主题色的通知，标题可选（默认不显示）。调用方**无需注入任何 bean**，内部经 Spring 上下文自取所需组件。

### 背景

提供跨视图/全局异常处理器的统一通知入口，避免各处自行 `new Notification(...)` 或直调 `Notifications.create()` 导致样式、位置、i18n 不一致。支持按类型配置自动关闭时长与显示位置（位置两级覆盖），标题国际化。

### API

```java
public final class NotificationUtil {

    // 单参数：默认不显示标题（受 notification-show-default-title 配置控制），
    // 配置为 true 时显示 i18n 默认标题；正文为 text
    public static void success(String text);
    public static void error(String text);
    public static void warning(String text);
    public static void info(String text);

    // 自定义标题 + 正文（显式标题始终显示，不受配置影响）
    public static void success(String title, String text);
    public static void error(String title, String text);
    public static void warning(String title, String text);
    public static void info(String title, String text);
}
```

### 使用示例

```java
import org.magic.jmix.addons.core.notification.NotificationUtil;

NotificationUtil.success("保存成功");
NotificationUtil.error("操作失败", "原因：" + e.getMessage());
```

### 配置项（magic.core.*）

通过 `application.properties` 配置。

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `magic.core.notification-success-duration` | 5000 | 成功通知自动关闭时长（毫秒）|
| `magic.core.notification-error-duration` | 8000 | 错误通知自动关闭时长（毫秒）|
| `magic.core.notification-warning-duration` | 8000 | 警告通知自动关闭时长（毫秒）|
| `magic.core.notification-info-duration` | 5000 | 信息通知自动关闭时长（毫秒）|
| `magic.core.notification-default-position` | BOTTOM_END | 全局默认显示位置 |
| `magic.core.notification-success-position` | （未配置）| 成功通知位置，未配置时回落 `default-position` |
| `magic.core.notification-error-position` | （未配置）| 错误通知位置，未配置时回落 `default-position` |
| `magic.core.notification-warning-position` | （未配置）| 警告通知位置，未配置时回落 `default-position` |
| `magic.core.notification-info-position` | （未配置）| 信息通知位置，未配置时回落 `default-position` |
| `magic.core.notification-show-default-title` | false | 是否显示默认标题（i18n 标题）；仅影响单参数 API，双参数显式标题始终显示 |

位置取值见 `com.vaadin.flow.component.notification.Notification.Position`（如 `BOTTOM_END`、`TOP_CENTER`、`TOP_STRETCH`、`MIDDLE` 等）。

**位置两级覆盖**：每个类型若配置了专属位置则使用之，否则回落 `notification-default-position`。普通用户只需配置 `notification-default-position` 全局生效；需要差异化的再额外配置按类型 position。

### 标题显示与国际化覆盖

**是否显示标题**：默认不显示。单参数 API（如 `success(text)`）是否显示 i18n 默认标题由 `notification-show-default-title` 控制（默认 `false`）；双参数 API（如 `success(title, text)`）传入的是显式自定义标题，始终显示、不受配置影响。

**标题文案**存放在 addon 根 messages 文件，key 形如 `org.magic.jmix.addons.core.notification/title.success`（中文 `系统提示`、英文 `Success` 等）。

项目若要覆盖标题文案，须在与 addon **同根包**的 messages 文件中写入对应 key（靠 classpath 顺序覆盖）。**在项目任意 messages 文件写跨包全限定 key 不生效**（Jmix `Messages` 查找固定在调用方包的根 messages）：

```properties
# 项目：src/main/resources/org/magic/jmix/addons/core/messages.properties
org.magic.jmix.addons.core.notification/title.success=操作成功
```

---

## MagicDetailWindowBuilderProcessor - 详情对话框列表回写守卫

自定义 `DetailWindowBuilderProcessor`，通过 `@Primary` Bean 替换 Jmix 框架默认实现，修复 **Dialog 模式**详情对话框未实际持久化时、仍用过期副本覆盖列表最新数据的问题。

### 背景 / 解决的场景

典型操作链路：打开某实体的详情对话框后，在列表上**直接对该行**做了操作（如停用/启用，列表与数据库都已更新为新状态），再回到详情页点「确定」。

此时详情对话框的 `DataContext` 仍持有打开时的**过期副本**，框架默认会在关闭时用这份过期副本执行 `container.replaceItem()`，把列表上刚更新的新状态**覆盖回旧值**——脏数据被带回列表。

### 原理

订阅详情 `DataContext` 的 `PostSaveEvent`，用 `getSavedInstances().isEmpty()` 判断这次关闭是否真正写入了数据库：

- **真正持久化**（详情数据最新、权威）：执行 `replaceItem`，列表更新为详情最新值；
- **未实际持久化**（详情数据可能过期、不可信）：**跳过 `replaceItem`**，保留列表原有数据。

新建实体（CREATE 模式）不受影响，照常加入列表；所有回调阶段（onSaved、afterSaved、afterSaveHandler）仍正常触发。

### 定位与生效方式

- **自动生效**：作为 `@Primary` Bean 替换框架 `DetailWindowBuilderProcessor`，凡经 `DialogWindows` 打开的详情对话框都走此实现，**无需任何配置或代码改动**。
- **仅限 Dialog 模式**：作用域是 `DialogWindows.detail(...)` / `@DialogMode` 打开的对话框；列表内联编辑、TAB 模式编辑不在范围内。
- **与 TAB 模式正交**：TAB 模式（标签页内编辑）的同等守卫在 `jmix-magic-addons-tab-layout` 的 `TabDetailProcessor` 中，两者互不影响。

### 调试

观察 persisted 判断与 replaceItem 取舍：

```properties
logging.level.org.magic.jmix.addons.core.view=DEBUG
```

---

## 增强 Action - 列表 CRUD 时序统一

替代标准 `list_create` / `list_edit` / `list_remove` 的三个增强 Action，统一列表视图在 **TAB 模式**（标签页内打开详情）与 **DIALOG 模式**（对话框打开详情）下的关闭事件时序，并兼容 Container 与 MetaClass 两种 Grid 绑定模式。

| Action 类 | typeId | 替代 |
|-----------|--------|------|
| `MagicCreateAction` | `magic_list_create` | `list_create` |
| `MagicEditAction` | `magic_list_edit` | `list_edit` |
| `MagicRemoveAction` | `magic_list_remove` | `list_remove` |

### 使用方式

XML 中把 action 的 `type` 指定为对应 typeId 即可，无需 Java 代码：

```xml
<actions>
    <action id="createAction" type="magic_list_create"/>
    <action id="editAction" type="magic_list_edit"/>
    <action id="removeAction" type="magic_list_remove"/>
</actions>
```

三个 Action 由 core 的 `magic_Actions` 自动扫描注册（扫描包 `org.magic.jmix.addons.core.action`），安装 core addon 后即可使用。

### DIALOG 模式：触发 DetailViewCloseCallback 三阶段

对话框关闭且为 SAVE 时，若列表视图实现了 [DetailViewCloseCallback](#detailviewclosecallback---详情关闭回调契约)，Create/Edit Action 按统一时序触发 Phase 1-3（`onClose` → `onSaved` → `afterSaved`），随后触发 Phase 4 的 `afterSaveHandler`（通过 Action 的 `withAfterSaveHandler(...)` 设置）。详见下节。

### TAB 模式：与 tab-layout addon 的路由桥接

TAB 模式下，Create/Edit Action 会向详情导航附加 `_pgrid`（Grid id）、`_mode`（edit）路由参数，并通过 `ComponentUtil` 把 `afterSaveHandler` 暂存到 Grid 组件上（key `magic_afterSaveHandler`）。

> 这是一套**隐式字符串契约**：消费方是 `jmix-magic-addons-tab-layout` 的 `TabDetailProcessor`，由它完成保存后的列表回填。
>
> - **安装了 tab-layout**：完整 TAB 回填行为生效；
> - **未安装 tab-layout**：Action 仍可正常打开详情，附加的参数/handler 无人消费，无害退化为标准导航。

即：这三个 Action 可独立于 tab-layout 使用；TAB 模式的增强回填需要 tab-layout 配合。

### MagicRemoveAction：兼容两种 Grid 模式

| Grid 模式 | 行为 |
|----------|------|
| **Container 模式**（含 `LazyContainerDataGridItems`） | 委托标准 `RemoveAction`，经 DataContext 删除 |
| **MetaClass 模式** | 用 `DataManager.remove()` 删除并刷新 DataProvider |

MetaClass Grid 的 SelectionModel 可能持有编辑前的旧实体（version 过期），直接删除会因 `em.merge()` 乐观锁不匹配抛异常。因此删除前先 `dataManager.load(Id.of(entity))` 重载最新版本，确保版本一致后再删除。

### Action 与列表视图的搭配

三个 Action 与列表视图基类的搭配决定完整能力。`BaseListView` / `BaseDetailView`（由 core addon 提供，见 [视图基类](#视图基类viewbase-包)）已实现 `DetailViewCloseCallback`；标准 `StandardListView` 不实现该接口。

| 功能 | StandardListView + 标准 Action | StandardListView + MagicAction | BaseListView + 标准 Action | BaseListView + MagicAction |
|------|------|------|------|------|
| **TAB 容器操作** | ✅ 标准 | ✅ 标准 | ✅ 标准（findFirstGrid 兜底） | ✅ 标准 |
| **TAB 精确路由（多 Grid）** | ❌ 无 `_pgrid` | ✅ `_pgrid` 精确定位 | ❌ 无 `_pgrid`，走 findFirstGrid | ✅ `_pgrid` 精确定位 |
| **TAB DetailViewCloseCallback** | — 不实现 | — 不实现 | ✅ 触发（容器可能为 null） | ✅ 触发 |
| **TAB afterSaveHandler** | ❌ 无桥接 | ✅ ComponentUtil 桥接 | ❌ 无桥接 | ✅ ComponentUtil 桥接 |
| **DIALOG 容器操作** | ✅ Jmix 标准 | ✅ Jmix 标准 | ✅ Jmix 标准 | ✅ Jmix 标准 |
| **DIALOG DetailViewCloseCallback** | — 不实现 | — 不实现 | ❌ 标准 Action 不触发回调 | ✅ MagicAction 触发回调 |
| **DIALOG scrollToIndex** | ❌ 无 | ❌ 无 | ❌ 无 | ✅ afterSaved 默认实现 |
| **DIALOG afterSaveHandler** | ✅ 标准支持 | ✅ 标准支持 | ✅ 标准支持 | ✅ 标准支持 |

- **推荐组合**：`BaseListView` + MagicAction，获得完整的四阶段回调和精确路由。
- **单 Grid 页面**：`BaseListView` + 标准 Action 的 TAB 模式也可正常工作（`findFirstGrid` 兜底），但 DIALOG 模式下 `DetailViewCloseCallback` 不会触发。

---

## DetailViewCloseCallback - 详情关闭回调契约

统一 DetailView 关闭后回调时序的接口，位于 `org.magic.jmix.addons.core.view.base.DetailViewCloseCallback`。

### 背景

标准 `list_create` / `list_edit` 在 DIALOG 模式下的关闭回调时序不统一、缺少"关闭即可中断"的入口。本接口定义四阶段时序，Phase 1-3 由增强 Action（DIALOG 模式）或 tab-layout 的 `TabDetailProcessor`（TAB 模式）触发，Phase 4 由 Action 或 `ComponentUtil` 桥接触发。

### 接口契约

```java
public interface DetailViewCloseCallback {

    /** Phase 1：任何关闭都触发。返回 false 中断，跳过后续阶段。 */
    default boolean onClose(CloseAction closeAction,
                            @Nullable Object entity,
                            @Nullable CollectionContainer<?> container,
                            @Nullable ListDataComponent<?> sourceGrid,
                            boolean isNew) {
        return true;
    }

    /** Phase 2：仅 SAVE 触发，处理数据逻辑。返回 false 跳过默认容器操作（如 MetaClass Grid 需自行 refreshAll）。 */
    default boolean onSaved(@Nullable Object entity,
                            @Nullable CollectionContainer<?> container,
                            @Nullable ListDataComponent<?> sourceGrid,
                            boolean isNew) {
        return true;
    }

    /** Phase 3：仅 SAVE 触发，处理 UI 逻辑。 */
    default void afterSaved(@Nullable Object entity,
                            @Nullable ListDataComponent<?> sourceGrid,
                            boolean isNew) {
        // 默认实现：新建时滚动 Grid 到顶部
        if (isNew && sourceGrid instanceof DataGrid<?> grid) {
            grid.scrollToIndex(0);
        }
    }
}
```

方法均为 `default`，按需覆写。

### 四阶段时序

| 阶段 | 方法 | 触发条件 | 用途 | 返回值 |
|------|------|----------|------|--------|
| Phase 1 | `onClose` | 任何关闭 | 可中断入口，返回 false 完全接管 | `boolean`：true 继续，false 中断 |
| Phase 2 | `onSaved` | 仅 SAVE | 数据逻辑（刷新数据源） | `boolean`：true 用默认容器操作，false 跳过 |
| Phase 3 | `afterSaved` | 仅 SAVE | UI 逻辑（滚动、选中、焦点） | void |
| Phase 4 | `afterSaveHandler` | 仅 SAVE | Action 级别的整个 ListView 后处理 | — |

### 覆写示例

列表视图实现接口并按需覆写各阶段。以继承 `BaseListView`（由 core addon 提供，基类已实现接口并提供默认行为）为例：

```java
public class UserListView extends BaseListView<User> {

    // Phase 1：关闭回调（任何关闭都触发）
    @Override
    public boolean onClose(CloseAction action, Object entity,
                           CollectionContainer<?> container,
                           ListDataComponent<?> grid, boolean isNew) {
        // 返回 true 继续后续阶段，返回 false 完全接管关闭逻辑
        return true;
    }

    // Phase 2：数据逻辑回调（仅 SAVE 触发）
    @Override
    public boolean onSaved(Object entity, CollectionContainer<?> container,
                           ListDataComponent<?> grid, boolean isNew) {
        // 返回 true 使用默认容器操作（add/replace），返回 false 跳过（如 MetaClass Grid）
        return true;
    }

    // Phase 3：UI 逻辑回调（仅 SAVE 触发）
    @Override
    public void afterSaved(Object entity, ListDataComponent<?> grid, boolean isNew) {
        // 默认行为：新建时滚动 Grid 到顶部；可覆写做额外 UI 操作（如选中、焦点）
    }
}
```

### TAB vs DIALOG 异同

| 阶段 | TAB 模式 | DIALOG 模式 |
|------|----------|-------------|
| Phase 1: onClose | ✅ | ✅ |
| Phase 2: onSaved return true | defaultAddToContainer() | 跳过（Jmix 已做） |
| Phase 2: onSaved return false | 跳过容器操作 | 跳过 |
| Phase 3: afterSaved | ✅ | ✅ |
| Phase 4: afterSaveHandler | ✅（ComponentUtil 桥接） | ✅（Action 直接调用） |

### 常见覆写模式

| 场景 | onClose | onSaved | afterSaved |
|------|---------|---------|------------|
| 默认（BaseListView） | return true | return true | 不需覆盖 |
| MetaClass Grid | return true | return false（onSaved 中 refreshAll） | 不需覆盖 |
| 完全自定义 | return false | — | — |
| 跳过容器操作，只做 UI | return true | return false | 自定义 UI 操作 |

### 谁应该实现

- 列表视图（`StandardListView`）需要在 DIALOG 模式统一关闭回调时，实现此接口并按需覆写；
- core 的 `BaseListView` / `BaseDetailView` 已实现。

### 参数说明

| 参数 | 说明 |
|------|------|
| `closeAction` | 关闭动作（`StandardOutcome.SAVE` / `CLOSE` / `DISCARD` 等） |
| `entity` | 被编辑的实体（未保存时为 null） |
| `container` | 列表数据容器（MetaClass Grid 时为 null） |
| `sourceGrid` | 触发导航的 Grid/List 组件（可能为 null） |
| `isNew` | 是否新建（CREATE 为 true，EDIT 为 false） |

---

## 多标签契约（tab 包） - 跨 addon 解耦

从 `jmix-magic-addons-tab-layout` 下沉到 core 的「视图生命周期事件 + 展示模式/行为声明」契约，位于 `org.magic.jmix.addons.core.tab` 包。

### 背景 / 为什么放在 core

TabLayout addon 提供多标签页架构，但它是**宿主框架级**的能力，其他 addon 不应直接依赖它。为了让任意 addon 能声明「我享受多标签特性」又保持松耦合，把跨 addon 共用的契约下沉到 core：

- 使用方只依赖 core，贴注解 / 实现接口即可声明多标签特性；
- 普通宿主（未安装 tab-layout）中：注解被忽略、事件不触发，但**无副作用、无报错**，视图正常工作；
- 安装了 tab-layout 的宿主中：自动获得多标签增强（标签页打开、激活/停用事件、不可关闭/多开声明等）。

### 契约清单

| 类 | 类型 | 说明 |
|----|------|------|
| `TabActivationAware` | 接口 | 视图 `implements` 后，可用 `@Subscribe` 监听激活/停用事件。提供 `addTabActivateListener` / `addTabDeactivateListener` 两个 default 方法 |
| `TabActivateEvent` | 事件 | 标签页被激活（用户切换到该标签页）时触发 |
| `TabDeactivateEvent` | 事件 | 标签页被停用（用户切换离开该标签页）时触发 |
| `PresentationMode` | 枚举 | 展示模式：`TAB` / `DIALOG` / `PAGE` / `INITIAL` |
| `@PresentationModes` | 注解 | 声明视图支持的展示模式，第一个为默认模式 |
| `PresentationModeHelper` | 工具类 | 解析视图支持的展示模式；`isManagedByMainView()` 判断视图是否受 MainView 管辖 |
| `@UncloseableTab` | 注解 | 声明标签页不可关闭（不显示关闭按钮） |
| `@MultipleOpenTab` | 注解 | 声明允许同一视图打开多个标签页实例 |

### 使用示例

#### 监听标签页激活/停用

```java
import org.magic.jmix.addons.core.tab.TabActivationAware;
import org.magic.jmix.addons.core.tab.TabActivateEvent;
import org.magic.jmix.addons.core.tab.TabDeactivateEvent;

public class FileListView extends StandardListView<File> implements TabActivationAware {

    @Subscribe
    public void onTabActivate(TabActivateEvent event) {
        // 标签页激活：刷新数据、启动轮询
        refreshData();
        refreshTimer.start();
    }

    @Subscribe
    public void onTabDeactivate(TabDeactivateEvent event) {
        // 标签页停用：停止轮询、保存临时状态
        refreshTimer.stop();
    }
}
```

> `TabActivationAware` 的两个 default 方法仅用于让 Jmix 的 `@Subscribe` 反射扫描能匹配到事件类型，无需手动调用。匹配机制是纯反射、按事件 `Class` 对象，不依赖包名。

#### 声明展示模式与标签行为

```java
import org.magic.jmix.addons.core.tab.annotation.PresentationMode;
import org.magic.jmix.addons.core.tab.annotation.PresentationModes;
import org.magic.jmix.addons.core.tab.annotation.UncloseableTab;

@PresentationModes({PresentationMode.TAB, PresentationMode.DIALOG})
@UncloseableTab
public class FileListView extends StandardListView<File> implements TabActivationAware {
    // 支持 TAB / DIALOG 打开；标签页不可关闭
}
```

### 事件触发机制

| 场景 | 行为 |
|------|------|
| 标签页切换（tab-layout 宿主） | `TabManager.activateTab()` 先 fire `TabDeactivateEvent`（旧视图），再 fire `TabActivateEvent`（新视图） |
| 标签页关闭 | 不触发激活/停用事件，走标准的 `BeforeCloseEvent` / `AfterCloseEvent` |
| 普通宿主（未装 tab-layout） | 事件不触发，但接口方法与注解均无害 |

### 谁应该使用

- **需要多标签页特性的 addon 视图**：依赖 core，实现 `TabActivationAware`、贴展示模式/行为注解即可，不感知 tab-layout 是否存在。
- **宿主项目视图**：同上。宿主若安装了 tab-layout，标签页系统的 `TabManager` 会读取这些契约并提供增强。

> 这套契约的**运行时实现**（标签页创建、激活、路由拦截、URL 同步等）仍在 `jmix-magic-addons-tab-layout` 的 `TabManager` / `TabRouterLayout` 等类中。core 只提供契约，不提供多标签运行时。

---

## 视图基类（view.base 包）

从 tab-layout 下沉到 core 的列表/详情视图基类，位于 `org.magic.jmix.addons.core.view.base`。

### BaseListView

列表视图基类，继承 `StandardListView<T>`，实现 `DetailViewCloseCallback` + `TabActivationAware`：

- 提供 [DetailViewCloseCallback](#detailviewclosecallback---详情关闭回调契约) 四阶段关闭回调的默认实现（`onClose` / `onSaved` / `afterSaved`），可按需覆写
- 内置 `TabActivationAware`，可 `@Subscribe` 监听 `TabActivateEvent` / `TabDeactivateEvent`
- `loadAllCollectionLoaders()` 手动触发视图中所有 CollectionLoader 加载

继承后配合增强 Action（`magic_list_create` / `magic_list_edit`），自动获得「新建加顶、编辑刷新单行」的智能数据刷新。

### BaseDetailView

详情视图基类，继承 `StandardDetailView<T>`，实现 `TabActivationAware`：

- 自动标题：新建显示「新建:实体名」，编辑显示「编辑:实体名:实例名」，只读显示「查看:实体名:实例名」
- 保存状态追踪：`isDataSaved()` / `isNewEntity()` / `getSavedEntity()` / `isSavedNewEntity()`
- 内置 `TabActivationAware`

### 自动标题消息

BaseDetailView 的标题前缀消息由 core 提供，可在项目消息文件中覆盖：

| 消息键 | 中文 | 英文 |
|--------|------|------|
| `org.magic.jmix.addons.core/detailView.title.new` | 新建 | New |
| `org.magic.jmix.addons.core/detailView.title.edit` | 编辑 | Edit |
| `org.magic.jmix.addons.core/detailView.title.view` | 查看 | View |

> `BaseListView` / `BaseDetailView` 与多标签事件契约、`DetailViewCloseCallback` 配合，在安装了 tab-layout 的宿主中获得完整的 TAB 模式 CRUD 时序；在普通宿主中作为标准视图基类正常工作。

---

## 版本兼容性

| Jmix 版本 | Vaadin 版本 | 插件版本 |
|-----------|-------------|----------|
| 2.8.1+ | 24.x | 0.0.1 |

## 更新日志

### 0.0.1
- 初始版本
- 新增 BaseEntity 实体基类（审计字段 + UUID 主键）
- 新增 AppConfig 应用配置实体
- 新增 AppConfigService 配置服务
- 新增增强 UI 组件：MagicGenericFilter、MagicSimplePagination（自动替换标准组件）
- 新增 LazyContainerDataGridItems 懒加载 DataGrid 数据提供器
- 新增 GenericUniqueConstraintViolationHandler 唯一约束异常处理器
- 新增 TreeGridScrollHelper 工具类
- 新增 NotificationUtil 统一通知工具（`magic.core.*` 配置、位置两级覆盖、标题 i18n、热修改兼容）
- 新增 MagicDetailWindowBuilderProcessor：`@Primary` 替换框架 `DetailWindowBuilderProcessor`，修复 Dialog 模式未持久化时脏数据回写列表（由 tab-layout addon 迁入）
- 新增强 Action：`magic_list_create` / `magic_list_edit` / `magic_list_remove`，统一 TAB/DIALOG 关闭回调时序、兼容 Container/MetaClass 两种 Grid 模式（由 tab-layout addon 迁入）
- 新增 DetailViewCloseCallback 详情关闭回调契约接口（四阶段：onClose/onSaved/afterSaved/afterSaveHandler）（由 tab-layout addon 迁入）

### 后续更新

- 新增多标签契约（`tab` 包），从 tab-layout addon 下沉到 core：`TabActivationAware` / `TabActivateEvent` / `TabDeactivateEvent`（标签页激活/停用事件契约）、`@PresentationModes` / `PresentationMode` / `PresentationModeHelper`（展示模式声明）、`@UncloseableTab` / `@MultipleOpenTab`（标签页行为声明）。让其他 addon 仅依赖 core 即可声明多标签特性，普通宿主中无副作用，安装 tab-layout 后自动获得增强。
- 新增视图基类 `BaseListView` / `BaseDetailView`（`org.magic.jmix.addons.core.view.base`），由 tab-layout addon 迁入。`BaseListView` 提供 `DetailViewCloseCallback` 四阶段关闭回调默认实现；`BaseDetailView` 提供自动标题与保存状态追踪；自动标题消息 `detailView.title.new/edit/view` 一并迁入 core。
- 新增 `CursorLazyGrid` 游标分页懒加载 Grid 组件（`CursorLazyGrid` / `CursorPagedDataProvider` / `CursorPage` / `CursorPageFetcher`），将游标顺序分页后端适配为 Grid 原生滚动懒加载，支持 undefined size 模式与异步 `enablePushUpdates`；注意其 DataProvider 为**全量缓存**（只增不减），大数据场景需评估内存（详见 USAGE 对应章节）。
