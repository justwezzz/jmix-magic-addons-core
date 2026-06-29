# Jmix Magic Core Addon

一个为 Jmix 2.x 提供通用组件增强的插件。此插件作为 Jmix Magic 系列插件的底座。

## 功能特性

- **BaseEntity** - 实体基类，提供统一的审计字段（创建时间、创建人、更新时间、更新人）和 UUID 主键风格
- **AppConfig** - 应用配置实体，用于键值对形式的配置存储
- **AppConfigService** - 配置服务，提供配置的读写操作
- **增强 UI 组件** - 通过组件注册机制自动替换标准 Jmix 组件：
  - `MagicGenericFilter` - 克隆过滤条件防止共享状态问题；添加清空按钮；应用时重置分页
  - `MagicSimplePagination` - 在数据刷新时保持 `firstResult` 位置，防止页码跳动
- **LazyContainerDataGridItems** - 懒加载 DataGrid 数据提供器，实现 DB 级分页
- **CursorLazyGrid** - 游标分页懒加载 Grid 组件，将游标顺序分页后端（nextCursor + hasMore）适配为 Grid 原生滚动懒加载，适用于 MinIO 搜索等游标分页、无精确总数的场景；异步加载防卡 UI，**注意其 DataProvider 为全量缓存，大数据场景需评估内存**
- **GenericUniqueConstraintViolationHandler** - 通用唯一约束异常处理器，自动解析约束并显示友好提示
- **TreeGridScrollHelper** - TreeGrid 滚动定位工具，支持滚动到指定节点位置
- **NotificationUtil** - 统一通知工具，按类型（success/error/warning/info）显示通知，时长/位置可配（`magic.core.*`），标题国际化
- **MagicDetailWindowBuilderProcessor** - 通过 `@Primary` Bean 替换框架默认 `DetailWindowBuilderProcessor`，修复 Dialog 模式详情页未实际持久化时仍把过期副本回写列表、覆盖列表最新数据的脏数据问题（自动生效，无需配置）
- **增强 Action**（`magic_list_create` / `magic_list_edit` / `magic_list_remove`）- 替代标准 `list_create`/`list_edit`/`list_remove`，统一 TAB/DIALOG 模式关闭回调时序、兼容 Container 与 MetaClass 两种 Grid 模式；TAB 模式与多标签页布局插件协作实现保存后列表回填
- **DetailViewCloseCallback** - DetailView 关闭回调契约接口（四阶段：`onClose`/`onSaved`/`afterSaved`/`afterSaveHandler`），列表视图实现后由增强 Action 在 DIALOG 模式触发，统一关闭后处理时序
- **多标签契约（tab 包）** -「视图生命周期事件 + 展示模式/行为声明」契约，让任意 addon 仅依赖 core 即可声明多标签特性：
  - `TabActivationAware` / `TabActivateEvent` / `TabDeactivateEvent` - 标签页激活/停用事件契约，视图 `implements TabActivationAware` 后用 `@Subscribe` 监听
  - `@PresentationModes` / `PresentationMode` / `PresentationModeHelper` - 视图展示模式声明（TAB/DIALOG/PAGE/INITIAL）
  - `@UncloseableTab` / `@MultipleOpenTab` - 标签页行为声明（不可关闭 / 允许多实例）

  这些契约在普通 Jmix 宿主中无人触发但无副作用；安装多标签页布局插件后自动获得多标签增强。详见 [USAGE.md](USAGE.md) 多标签契约章节。
- **视图基类（view.base 包）** - 列表/详情视图基类：
  - `BaseListView<T>` - 列表视图基类，实现 `DetailViewCloseCallback` + `TabActivationAware`，提供四阶段关闭回调默认实现
  - `BaseDetailView<T>` - 详情视图基类，实现 `TabActivationAware`，提供自动标题（新建/编辑/查看）与保存状态追踪

  继承即可获得智能数据刷新（新建加顶 / 编辑刷新单行）与自动标题，配合增强 Action 实现 TAB/DIALOG 一致的 CRUD 时序。
- **Jmix 框架翻译补充（基于 2.8）** - 补充 Jmix 框架未翻译的中文消息（分页、GenericFilter、连接状态指示器等），安装即生效

## 快速开始

### 1. 添加依赖

```groovy
dependencies {
    implementation 'io.github.justwezzz:jmix-magic-addons-core-starter:0.0.1'
}
```

### 2. 配置 Liquibase

在宿主项目 `changelog.xml` 中添加：

```xml
<include file="org/magic/jmix/addons/core/liquibase/changelog.xml"/>
```

> 详细说明见 [USAGE.md](USAGE.md)。

## 文档

详细使用指南请参阅 [USAGE.md](USAGE.md)。

## 版本要求

| 依赖 | 版本 |
|------|------|
| Jmix | 2.8.1+ |
| Java | 17+ |
| Vaadin | 24.x |

## 许可证

Apache License 2.0
