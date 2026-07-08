# Jmix Magic Core Addon

[中文文档](README.md) | English

A plugin providing common component enhancements for Jmix 2.x. This plugin serves as the foundation for the Jmix Magic addon series.

## Features

- **BaseEntity** - Entity base class providing unified audit fields (create time, created by, update time, updated by) and UUID primary key style
- **AppConfig** - Application configuration entity for key-value pair configuration storage
- **AppConfigService** - Configuration service providing read/write operations for configurations
- **Enhanced UI Components** - Automatically replace standard Jmix components via component registration mechanism:
  - `MagicGenericFilter` - Clone filter conditions to prevent shared state issues; add clear button; reset pagination on apply
  - `MagicSimplePagination` - Maintain `firstResult` position during data refresh to prevent page jumping
- **LazyContainerDataGridItems** - Lazy loading DataGrid data provider implementing DB-level pagination
- **CursorLazyGrid** - Cursor pagination lazy loading Grid component, adapting cursor-based sequential pagination backend (nextCursor + hasMore) to Grid native scroll lazy loading, suitable for MinIO search and other cursor pagination scenarios without precise total count; async loading prevents UI freezing, **note that its DataProvider is fully cached, evaluate memory for large data scenarios**
- **GenericUniqueConstraintViolationHandler** - Generic unique constraint violation handler, automatically parses constraints and displays friendly messages
- **TreeGridScrollHelper** - TreeGrid scroll positioning utility, supporting scroll to specified node position
- **NotificationUtil** - Unified notification utility, displays notifications by type (success/error/warning/info), configurable duration/position (`magic.core.*`), i18n titles
- **MagicDetailWindowBuilderProcessor** - Replaces framework default `DetailWindowBuilderProcessor` via `@Primary` bean, fixes dirty data issue where Dialog mode detail view overwrites list with stale copy when not actually persisted (automatically effective, no configuration needed)
- **Enhanced Actions** (`magic_list_create` / `magic_list_edit` / `magic_list_remove`) - Replace standard `list_create`/`list_edit`/`list_remove`, unify TAB/DIALOG mode close callback timing, compatible with both Container and MetaClass Grid modes; TAB mode collaborates with multi-tab layout plugin for post-save list backfill
- **DetailViewCloseCallback** - DetailView close callback contract interface (four phases: `onClose`/`onSaved`/`afterSaved`/`afterSaveHandler`), implemented by list views and triggered by enhanced Actions in DIALOG mode to unify post-close handling timing
- **Multi-Tab Contracts (tab package)** - "View lifecycle events + presentation mode/behavior declaration" contracts, allowing any addon to declare multi-tab features by only depending on core:
  - `TabActivationAware` / `TabActivateEvent` / `TabDeactivateEvent` - Tab activation/deactivation event contracts, views `implements TabActivationAware` then listen with `@Subscribe`
  - `@PresentationModes` / `PresentationMode` / `PresentationModeHelper` - View presentation mode declaration (TAB/DIALOG/PAGE/INITIAL)
  - `@UncloseableTab` / `@MultipleOpenTab` - Tab behavior declaration (non-closeable / allow multiple instances)

  These contracts are not triggered in plain Jmix hosts but have no side effects; automatically gain multi-tab enhancements after installing multi-tab layout plugin. See [USAGE_EN.md](USAGE_EN.md) multi-tab contracts section.
- **Cross-View Navigation Contracts (view.navigation package)** - Decoupling solution for cross-view data passing:
  - `ViewNavigationSupport` - Contract interface, defines `open(viewClass)` method, returns `ViewNavigationBuilder`
  - `ViewNavigationBuilder<V>` - Builder base class, provides `withAfterViewCreated` / `withAfterViewClosed` / `navigate` fluent API
  - `AfterViewClosedEvent<V>` - Close event, provides `getView()` / `getCloseAction()` / `closedWith(StandardOutcome)`
  - `DefaultViewNavigationSupport` - Default implementation (uses Jmix viewNavigators), registered with `@ConditionalOnMissingBean`

  After installing multi-tab layout plugin, automatically gains enhancements with callbacks working correctly under multi-tab architecture. Any module depending on Core addon can inject `ViewNavigationSupport` for cross-view data passing without knowing who provides the implementation.
- **View Base Classes (view.base package)** - List/detail view base classes:
  - `BaseListView<T>` - List view base class, implements `DetailViewCloseCallback` + `TabActivationAware`, provides default implementations for four-phase close callbacks
  - `BaseDetailView<T>` - Detail view base class, implements `TabActivationAware`, provides auto titles (new/edit/view) and save state tracking

  Inherit to get smart data refresh (new items at top / edit refresh single row) and auto titles, work with enhanced Actions for consistent TAB/DIALOG CRUD timing.
- **Jmix Framework Translation Supplement (based on 2.8)** - Supplements untranslated Chinese messages in Jmix framework (pagination, GenericFilter, connection indicator, etc.), effective upon installation

## Quick Start

### 1. Add Dependency

```groovy
dependencies {
    implementation 'io.github.justwezzz:jmix-magic-addons-core-starter:0.0.2'
}
```

### 2. Configure Liquibase

Add to your project's `changelog.xml`:

```xml
<include file="org/magic/jmix/addons/core/liquibase/changelog.xml"/>
```

> See [USAGE_EN.md](USAGE_EN.md) for detailed instructions.

## Documentation

For detailed usage guide, see [USAGE_EN.md](USAGE_EN.md).

## Version Requirements

| Dependency | Version |
|------------|---------|
| Jmix | 2.8.1+ |
| Java | 17+ |
| Vaadin | 24.x |

## License

Apache License 2.0
