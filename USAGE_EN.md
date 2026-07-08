# Jmix Magic Core Addon - Usage Guide

[中文文档](USAGE.md) | English

This document provides detailed usage instructions for the `jmix-magic-addons-core` plugin.

## Installation

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

---

## Table of Contents

1. [Installation](#installation)
2. [BaseEntity - Entity Base Class](#baseentity---entity-base-class)
3. [AppConfig - Application Configuration](#appconfig---application-configuration)
4. [AppConfigService - Configuration Service](#appconfigservice---configuration-service)
5. [Enhanced UI Components](#enhanced-ui-components)
6. [LazyContainerDataGridItems - Lazy Loading DataGrid](#lazycontainerdatagriditems---lazy-loading-datagrid)
7. [CursorLazyGrid - Cursor Pagination Lazy Loading Grid](#cursorlazygrid---cursor-pagination-lazy-loading-grid)
8. [GenericUniqueConstraintViolationHandler - Unique Constraint Violation Handling](#genericuniqueconstraintviolationhandler---unique-constraint-violation-handling)
9. [TreeGridScrollHelper - TreeGrid Scroll Positioning](#treegridscrollhelper---treegrid-scroll-positioning)
10. [NotificationUtil - Unified Notification Utility](#notificationutil---unified-notification-utility)
11. [MagicDetailWindowBuilderProcessor - Detail Dialog List Backwrite Guard](#magicdetailwindowbuilderprocessor---detail-dialog-list-backwrite-guard)
12. [Enhanced Actions - List CRUD Timing Unification](#enhanced-actions---list-crud-timing-unification)
13. [DetailViewCloseCallback - Detail Close Callback Contract](#detailviewclosecallback---detail-close-callback-contract)
14. [Multi-Tab Contracts (tab package) - Cross-addon Decoupling](#multi-tab-contracts-tab-package---cross-addon-decoupling)
15. [Cross-View Navigation Contracts (view.navigation package) - Cross-View Data Passing Decoupling](#cross-view-navigation-contracts-viewnavigation-package---cross-view-data-passing-decoupling)
16. [View Base Classes (view.base package)](#view-base-classes-viewbase-package)
17. [Jmix Framework Translation Supplement](#jmix-framework-translation-supplement)

---

## BaseEntity - Entity Base Class

Unified entity base class. All project entities should extend this class to ensure consistent audit functionality and ID style.

### Provided Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key, `@JmixGeneratedValue` |
| `version` | Integer | Version number, `@Version`, optimistic lock |
| `createTs` | Date | Creation time, `@CreatedDate` |
| `createdBy` | String | Created by, `@CreatedBy` |
| `updateTs` | Date | Update time, `@LastModifiedDate` |
| `updatedBy` | String | Updated by, `@LastModifiedBy` |

### Usage

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

### Best Practices

1. **All entities must extend BaseEntity** - Maintain project consistency
2. **Use Metadata.create() for instantiation** - Do not use constructors
3. **Audit fields are auto-filled** - No manual setting needed

---

## AppConfig - Application Configuration

Key-value pair application configuration entity for storing system-level configurations.

### Entity Structure

| Field | Type | Length | Description |
|-------|------|--------|-------------|
| `configKey` | String | 32 | Configuration key, unique |
| `configValue` | String | 256 | Configuration value |
| `description` | String | 256 | Description (optional) |

### Database Table

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

## AppConfigService - Configuration Service

Service providing read/write operations for configurations.

### API

```java
public interface AppConfigService {

    /**
     * Get configuration value
     * @param key Configuration key
     * @return Configuration value, returns null if not exists
     */
    String getValue(String key);

    /**
     * Set configuration value (update if exists, create if not)
     * @param key Configuration key
     * @param value Configuration value
     */
    void setValue(String key, String value);

    /**
     * Set configuration value with description
     */
    void setValue(String key, String value, String description);

    /**
     * Delete configuration
     * @param key Configuration key
     */
    void delete(String key);

    /**
     * Check if configuration exists
     * @param key Configuration key
     * @return Whether exists
     */
    boolean exists(String key);
}
```

### Usage Example

```java
@Autowired
private AppConfigService appConfigService;

// Read configuration
String systemName = appConfigService.getValue("system.name");
if (systemName == null) {
    systemName = "Default System Name"; // Provide default value
}

// Write configuration
appConfigService.setValue("system.name", "Magic Platform");

// Write configuration with description
appConfigService.setValue("system.version", "1.0.0", "System version number");

// Check if exists
if (!appConfigService.exists("system.initialized")) {
    // Initialization logic...
    appConfigService.setValue("system.initialized", "true", "Whether system is initialized");
}

// Delete configuration
appConfigService.delete("system.temp");
```

### Use Cases

- System initialization flags
- Dynamic configuration (effective without restart)
- Feature toggles
- System version information

---

## Enhanced UI Components

The plugin provides two enhanced components that **automatically replace** standard Jmix components via component registration mechanism.

### MagicGenericFilter

Replaces standard `GenericFilter`, fixing the following issues:

| Issue | Solution |
|-------|----------|
| Condition sharing when `autoApply=false` | Deep clone conditions when applying to avoid multiple view instances sharing the same condition object |
| No clear button | Add clear button (eraser icon) to clear all filter conditions with one click |
| Pagination not reset after applying conditions | Automatically reset `firstResult` to 0 when applying filter conditions |

**Usage**:

Use standard tag in XML, no changes needed:

```xml
<genericFilter id="genericFilter" dataLoader="entitiesDl" opened="false">
    <!-- Filter condition configuration -->
</genericFilter>
```

The plugin automatically maps `<genericFilter>` to `MagicGenericFilter` via `ComponentRegistration.replaceComponent()`.

### MagicSimplePagination

Replaces standard `SimplePagination`, fixing page jumping issue after data refresh:

| Issue | Solution |
|-------|----------|
| Reset to first page after data refresh | Record `committedFirstResult` in `onRefreshItems`, restore during pagination operations |

**Usage**:

Use standard tag in XML, no changes needed:

```xml
<simplePagination id="pagination" dataLoader="entitiesDl"/>
```

The plugin automatically maps `<simplePagination>` to `MagicSimplePagination` via `ComponentRegistration.replaceComponent()`.

---

## LazyContainerDataGridItems - Lazy Loading DataGrid

Lazy loading DataGrid data provider, wrapping standard `CollectionContainer` to implement DB-level pagination.

### Core Features

- Implements `ContainerDataUnit`, fully compatible with standard CC event mechanism and DetailView flow
- New entities automatically added to `prependBuffer`, displayed at list top
- Supports `genericFilter`, sorting, `baseCondition`
- Loads data from DB on-demand during scrolling, CC only holds current viewport data

### Usage

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

### Builder Configuration Options

| Method | Description |
|--------|-------------|
| `pageSize(int)` | Page size, defaults from `magic.core.lazy-page-size` (default 50) |
| `totalCountLabel(Span)` | Total count display label, shows "1-30 / 100" format |
| `afterDataChange(Runnable)` | Callback after data change (e.g., after scroll loading completes) |
| `baseConditionSupplier(Supplier<Condition>)` | Base condition supplier for filtering detail data in master-detail scenarios |

### Configuration

```properties
# Lazy loading page size
magic.core.lazy-page-size=50
```

### Data Flow

1. `fetch()` gets data from `prependBuffer` and DB, DB data synchronized to CC via `DataGenerator` callback
2. New entity triggers CC's `ADD_ITEMS` event, captured to `prependBuffer` and removed from CC to avoid duplication
3. Edit triggers CC's `SET_ITEM` event, updates corresponding entity reference in `prependBuffer`
4. Delete triggers CC's `REMOVE_ITEMS` event, removes corresponding entity from `prependBuffer`

---

## CursorLazyGrid - Cursor Pagination Lazy Loading Grid

Cursor pagination lazy loading Grid component, wrapping custom `AbstractDataProvider`, adapting **cursor-based sequential pagination** backend (pass cursor, return `items + nextCursor + hasMore`) to Grid's native scroll lazy loading. Suitable for MinIO search, object storage listing, and other scenarios where **the backend itself uses cursor pagination and has no precise total count**.

### Difference from LazyContainerDataGridItems

| Dimension | LazyContainerDataGridItems | CursorLazyGrid |
|-----------|----------------------------|----------------|
| Data source | JPA/DB (DataManager + JPQL offset/count) | Arbitrary cursor pagination backend (fetcher callback) |
| Pagination model | offset random access (can jump pages, can look back) | cursor sequential forward (can only advance) |
| Total count | Precise count | No totalCount (undefined size mode) |
| Cache | CC viewport cache (collected when scrolled away, re-fetched on demand) | **Full cache (only grows, see below)** |
| Grid type | Jmix `DataGrid` (requires dataContainer) | Native Vaadin `Grid` (also compatible with DataGrid) |

### Core Features

- Custom `AbstractDataProvider`, cursor sequential stream → Grid offset lazy loading
- Undefined size mode: automatically loads next page when scrolling to bottom, convergence by `fetch` returning less than expected (doesn't depend on size estimation)
- Async `enablePushUpdates`: backend blocking IO doesn't freeze UI thread (requires host to enable `@Push`)
- install/Builder one-line integration, default pageSize reads from `magic.core.lazy-page-size`

### Usage

```java
import org.magic.jmix.addons.core.component.CursorLazyGrid;
import org.magic.jmix.addons.core.component.CursorPagedDataProvider;
import org.magic.jmix.addons.core.component.CursorPage;
import org.magic.jmix.addons.core.component.CursorPageFetcher;

// 1. Define cursor pagination callback: backend returns one page by cursor (cursor=null means start from beginning)
CursorPageFetcher<Item> fetcher = cursor -> {
    SearchResult r = backend.searchPaged(keyword, cursor, 50);  // Your cursor pagination backend
    return CursorPage.of(r.getItems(), r.getNextCursor(), r.isHasMore());
};

// 2. Install to native Grid (Jmix DataGrid also works, upcast for compatibility)
CursorPagedDataProvider<Item> provider =
        CursorLazyGrid.install(grid, fetcher)
                .pageSize(50)
                .apply();

// 3. Reset when switching search conditions (clear cache, reset cursor, reload)
provider.reset();
```

### Builder Configuration Options

| Method | Description |
|--------|-------------|
| `pageSize(int)` | Page size, defaults from `magic.core.lazy-page-size` (default 50) |
| `executor(ExecutorService)` | Custom async executor; if not provided, component creates one and auto-shutdowns on Grid detach |

### ⚠️ Cache Mechanism: Full Cache, Evaluate for Large Data Scenarios

> **This is the most important feature users need to know about this component.**

`CursorPagedDataProvider` internally maintains a **grow-only** loaded cache `loaded`. Each time a new page is loaded during scrolling, data is appended to this cache, **never collected**.

**Why full cache is necessary**: Cursor can only move forward sequentially, cannot go backward. When user scrolls back up to previously viewed area, Grid will re-request data at that offset, but cursor cannot "rewind" to re-fetch — so all loaded data must be retained in cache for reverse requests to hit. This is fundamentally different from Vaadin's native `BackEndDataProvider` (offset can be randomly re-fetched, no cache).

**Direct consequences**:

- **Scroll to bottom = full load into memory**. As user keeps scrolling, all matching items eventually accumulate in `loaded` cache, equivalent to full loading (just in batches).
- Won't save memory due to Grid viewport collection — Grid layer indeed only displays near viewport, but provider layer keeps everything.

**⚠️ Use cautiously in large data scenarios**:

- Suitable for **controllable result sets** (search matches typically hundreds to thousands, full cache is no pressure).
- If backend may return **massive results (tens of thousands or more)**, user scrolling to bottom will cause memory to keep growing, **please evaluate carefully**; if necessary, add limit in fetcher yourself (stop after N items with `hasMore=false`).
- If you need true "streaming memory efficiency" (only cache near viewport, collect when scrolled away), use data source with random offset access — i.e., **`LazyContainerDataGridItems` (JPA/DB)**, not this component.

### Suitable / Unsuitable Scenarios

| Suitable ✅ | Unsuitable ❌ |
|------------|---------------|
| MinIO / object storage search (cursor pagination, no total count) | JPA/DB large table pagination (use `LazyContainerDataGridItems`) |
| Controllable result sets (hundreds~thousands) of cursor data | Potentially massive cursor data (memory risk without limit) |
| Backend only has cursor API, no offset/count | Scenarios requiring precise total count or sortable (cursor order is fixed) |

---

## GenericUniqueConstraintViolationHandler - Unique Constraint Violation Handling

Automatically intercepts unique constraint violations and displays friendly messages.

### Supported Constraint Definition Methods

| Method | Supported | Description |
|--------|-----------|-------------|
| `@Index(name = "IDX_XXX", columnList = "COL", unique = true)` | ✅ | Recommended |
| `@UniqueConstraint(name = "UK_XXX", columnNames = "COL")` | ✅ | Recommended |
| `@Column(unique = true)` | ❌ | Not supported, database auto-generates constraint name |

### Working Principle

1. Scan all entity unique constraint definitions at startup
2. Catch database unique constraint exceptions
3. Parse constraint name, find corresponding entity and field
4. Display friendly message

### Custom Messages

Default message: `A {entity} with the same {field} already exists`

Customize in your project's `messages.properties`:

```properties
# Customize message for specific constraint
databaseUniqueConstraintViolation.IDX_USER_USERNAME=Username already exists, please use another username

# Modify default template
org.magic.jmix.addons.core/uniqueConstraintViolation.default=The {0} of this {1} already exists

# Multi-field connector
org.magic.jmix.addons.core/fieldConnector=and
```

### Correctly Defining Unique Constraints

```java
@Table(name = "MAGIC_USER", indexes = {
    @Index(name = "IDX_USER_USERNAME", columnList = "USERNAME", unique = true),
    @Index(name = "IDX_USER_EMAIL", columnList = "EMAIL", unique = true)
})
public class User extends BaseEntity {
    // ...
}
```

### Foreign Key Constraint Violation Handling (GenericForeignKeyViolationHandler)

Complementing the unique constraint handler, `GenericForeignKeyViolationHandler` (`@Component("magic_GenericForeignKeyViolationHandler")`) intercepts database **foreign key constraint violation** exceptions (triggered when deleting/updating referenced records), parses foreign key constraint name and displays friendly message.

- **Message prefix**: `databaseForeignKeyViolation.`, default template key `databaseForeignKeyViolation.default`
- **Constraint name regex**: Default matches Chinese/English foreign key constraint exception text; can customize with `magic.db.foreignKeyViolationPattern` configuration
- **Custom message**: Override by constraint name in project messages, e.g., `databaseForeignKeyViolation.FK_ORDER_CUSTOMER=This order is referenced by customer, cannot delete`

### Constraint Metadata Scanning (ConstraintMetadataScanner / ConstraintInfo)

`ConstraintMetadataScanner` (`@Component("magic_ConstraintMetadataScanner")`) scans all entities' `@Table` / `@UniqueConstraint` annotations at application startup (`ContextRefreshedEvent`), builds "constraint name → entity/field" mapping cache (`ConstraintInfo`), for above two constraint handlers to locate entity and field by database-returned constraint name at runtime.

---

## TreeGridScrollHelper - TreeGrid Scroll Positioning

TreeGrid scroll positioning utility class for scrolling to specified node position in TreeGrid.

### Background

Vaadin TreeGrid uses virtual scrolling mechanism, directly setting `scrollTop` cannot properly trigger virtual scroll update. `TreeGridScrollHelper` wraps the correct scrolling method, using Grid's internal `_scrollToFlatIndex` method to implement scrolling.

### API

```java
public class TreeGridScrollHelper {

    /**
     * Scroll TreeGrid to specified node position (simple scenario)
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode);

    /**
     * Scroll TreeGrid to specified node position (with callback)
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode,
            Consumer<Integer> callback);

    /**
     * Scroll TreeGrid to specified node position (full parameters)
     *
     * @param filter Filter, returns true if node participates in index calculation,
     *               can be null (no filter). Used to filter out unwanted nodes like placeholders
     */
    public static <T> Optional<Integer> scrollToNode(
            TreeDataGrid<T> grid,
            TreeData<T> treeData,
            T targetNode,
            Predicate<T> filter,
            Consumer<Integer> callback);
}
```

### Usage Examples

#### Simple Scenario (No Placeholders)

```java
Optional<Integer> index = TreeGridScrollHelper.scrollToNode(
    grid,
    treeData,
    targetNode
);

if (index.isEmpty()) {
    showNotification("Node not found");
}
```

#### Lazy Loading Scenario (Filter Placeholders)

```java
TreeGridScrollHelper.scrollToNode(
    grid,
    treeData,
    targetNode,
    node -> !node.getPath().endsWith(".placeholder"),  // Filter placeholders
    null
);
```

#### With Callback

```java
TreeGridScrollHelper.scrollToNode(
    grid,
    treeData,
    targetNode,
    idx -> {
        // Execute after scrolling completes
        showNotification("Located at node, index: " + idx);
    }
);
```

### Prerequisites

Caller needs to ensure:

1. **Target node is visible in expanded tree**: If target node's parent is not expanded, need to expand parent first
2. **Data contains no placeholders**: If using lazy loading placeholder mode, need to ensure TreeData doesn't contain placeholder nodes

### Implementation Principle

1. Traverse TreeData to calculate target node's flat index in current expansion state
2. Use JavaScript to call Grid's internal `_scrollToFlatIndex` method
3. Delay 50ms for JS execution to ensure Grid internal state sync completes
4. Callback triggers after 100ms delay from scrolling

### Notes

- `_scrollToFlatIndex` is Vaadin Grid's internal method, may change in future versions
- Delay times (50ms/100ms) are empirical values, may need adjustment in extreme cases
- Returned index can be used for debug logging

---

## NotificationUtil - Unified Notification Utility

Unified notification utility class, displays notifications with body and theme color by type (success / error / warning / info), title optional (default not shown). Caller **doesn't need to inject any bean**, internally gets required components via Spring context.

### Background

Provides unified notification entry point across views/global exception handlers, avoiding each place creating `new Notification(...)` or directly calling `Notifications.create()` causing inconsistent style, position, i18n. Supports auto-close duration and display position configuration by type (position two-level override), title i18n.

### API

```java
public final class NotificationUtil {

    // Single parameter: default no title (controlled by notification-show-default-title config),
    // when configured as true shows i18n default title; body is text
    public static void success(String text);
    public static void error(String text);
    public static void warning(String text);
    public static void info(String text);

    // Custom title + body (explicit title always shown, not affected by config)
    public static void success(String title, String text);
    public static void error(String title, String text);
    public static void warning(String title, String text);
    public static void info(String title, String text);
}
```

### Usage Example

```java
import org.magic.jmix.addons.core.notification.NotificationUtil;

NotificationUtil.success("Saved successfully");
NotificationUtil.error("Operation failed", "Reason: " + e.getMessage());
```

### Configuration (magic.core.*)

Configure via `application.properties`.

| Configuration | Default | Description |
|---------------|---------|-------------|
| `magic.core.notification-success-duration` | 5000 | Success notification auto-close duration (milliseconds) |
| `magic.core.notification-error-duration` | 8000 | Error notification auto-close duration (milliseconds) |
| `magic.core.notification-warning-duration` | 8000 | Warning notification auto-close duration (milliseconds) |
| `magic.core.notification-info-duration` | 5000 | Info notification auto-close duration (milliseconds) |
| `magic.core.notification-default-position` | BOTTOM_END | Global default display position |
| `magic.core.notification-success-position` | (not configured) | Success notification position, falls back to `default-position` if not configured |
| `magic.core.notification-error-position` | (not configured) | Error notification position, falls back to `default-position` if not configured |
| `magic.core.notification-warning-position` | (not configured) | Warning notification position, falls back to `default-position` if not configured |
| `magic.core.notification-info-position` | (not configured) | Info notification position, falls back to `default-position` if not configured |
| `magic.core.notification-show-default-title` | false | Whether to show default title (i18n title); only affects single-parameter API, two-parameter explicit title always shown |

Position values see `com.vaadin.flow.component.notification.Notification.Position` (e.g., `BOTTOM_END`, `TOP_CENTER`, `TOP_STRETCH`, `MIDDLE`, etc.).

**Position two-level override**: Each type uses dedicated position if configured, otherwise falls back to `notification-default-position`. Regular users only need to configure `notification-default-position` for global effect; configure per-type position for differentiation.

### Title Display and i18n Override

**Whether to show title**: Default not shown. Single-parameter API (e.g., `success(text)`) whether to show i18n default title is controlled by `notification-show-default-title` (default `false`); two-parameter API (e.g., `success(title, text)`) passes explicit custom title, always shown, not affected by config.

**Title text** is stored in addon root messages file, key like `org.magic.jmix.addons.core.notification/title.success` (Chinese `系统提示`, English `Success`, etc.).

To override title text, project must write corresponding key in messages file at **same root package** as addon (relies on classpath order to override). **Writing cross-package fully-qualified key in arbitrary project messages file won't work** (Jmix `Messages` lookup is fixed at caller package's root messages):

```properties
# Project: src/main/resources/org/magic/jmix/addons/core/messages.properties
org.magic.jmix.addons.core.notification/title.success=Operation Successful
```

---

## MagicDetailWindowBuilderProcessor - Detail Dialog List Backwrite Guard

Custom `DetailWindowBuilderProcessor`, replaces Jmix framework default implementation via `@Primary` bean, fixes issue where **Dialog mode** detail dialog overwrites list with stale data when not actually persisted.

### Background / Solved Scenario

Typical operation chain: Open detail dialog for an entity, then **directly operate on that row** in the list (e.g., disable/enable, list and database both updated to new state), then go back to detail page and click "OK".

At this point, detail dialog's `DataContext` still holds **stale copy** from when opened, framework default will execute `container.replaceItem()` with this stale copy on close, overwriting the just-updated new state in list **back to old value** — dirty data brought back to list.

### Principle

Subscribe to detail `DataContext`'s `PostSaveEvent`, use `getSavedInstances().isEmpty()` to determine if this close actually wrote to database:

- **Actually persisted** (detail data is latest, authoritative): Execute `replaceItem`, list updates to detail's latest value;
- **Not actually persisted** (detail data may be stale, untrustworthy): **Skip `replaceItem`**, keep list's original data.

New entity (CREATE mode) not affected, added to list as usual; all callback phases (onSaved, afterSaved, afterSaveHandler) still trigger normally.

### Positioning and Activation

- **Automatically effective**: As `@Primary` bean replaces framework `DetailWindowBuilderProcessor`, any detail dialog opened via `DialogWindows` goes through this implementation, **no configuration or code changes needed**.
- **Dialog mode only**: Scope is dialogs opened via `DialogWindows.detail(...)` / `@DialogMode`; inline list editing, TAB mode editing not in scope.
- **Orthogonal to TAB mode**: TAB mode (in-tab editing) equivalent guard is in multi-tab layout plugin's `TabDetailProcessor`, they don't affect each other.

### Debugging

Observe persisted judgment and replaceItem decision:

```properties
logging.level.org.magic.jmix.addons.core.view=DEBUG
```

---

## Enhanced Actions - List CRUD Timing Unification

Three enhanced Actions replacing standard `list_create` / `list_edit` / `list_remove`, unifying list view close event timing in **TAB mode** (open detail in tab) and **DIALOG mode** (open detail in dialog), and compatible with both Container and MetaClass Grid binding modes.

| Action Class | typeId | Replaces |
|--------------|--------|----------|
| `MagicCreateAction` | `magic_list_create` | `list_create` |
| `MagicEditAction` | `magic_list_edit` | `list_edit` |
| `MagicRemoveAction` | `magic_list_remove` | `list_remove` |

### Usage

Specify action's `type` as corresponding typeId in XML, no Java code needed:

```xml
<actions>
    <action id="createAction" type="magic_list_create"/>
    <action id="editAction" type="magic_list_edit"/>
    <action id="removeAction" type="magic_list_remove"/>
</actions>
```

Three Actions are auto-registered by core's `magic_Actions` (scans package `org.magic.jmix.addons.core.action`), available after installing core addon.

### DIALOG Mode: Triggers DetailViewCloseCallback Three Phases

When dialog closes with SAVE, if list view implements [DetailViewCloseCallback](#detailviewclosecallback---detail-close-callback-contract), Create/Edit Action triggers Phase 1-3 in unified timing (`onClose` → `onSaved` → `afterSaved`), then triggers Phase 4 `afterSaveHandler` (set via Action's `withAfterSaveHandler(...)`). See next section.

### TAB Mode: Route Bridging with Multi-Tab Layout Plugin

In TAB mode, Create/Edit Action attaches `_pgrid` (Grid id), `_mode` (edit) route parameters to detail navigation, and temporarily stores `afterSaveHandler` on Grid component via `ComponentUtil` (key `magic_afterSaveHandler`).

> This is an **implicit string contract**: Consumer is multi-tab layout plugin's `TabDetailProcessor`, which completes post-save list backfill.
>
> - **Multi-tab layout plugin installed**: Full TAB backfill behavior effective;
> - **Multi-tab layout plugin not installed**: Action can still open detail normally, attached parameters/handler have no consumer, harmlessly degrade to standard navigation.

That is: These three Actions can be used independently of multi-tab layout plugin; TAB mode enhanced backfill requires multi-tab layout plugin cooperation.

### MagicRemoveAction: Compatible with Two Grid Modes

| Grid Mode | Behavior |
|-----------|----------|
| **Container mode** (including `LazyContainerDataGridItems`) | Delegates to standard `RemoveAction`, deletes via DataContext |
| **MetaClass mode** | Uses `DataManager.remove()` to delete and refresh DataProvider |

MetaClass Grid's SelectionModel may hold pre-edit old entity (version stale), direct delete would throw exception due to `em.merge()` optimistic lock mismatch. Therefore before deleting, first `dataManager.load(Id.of(entity))` to reload latest version, ensure version matches before deleting.

### Action and List View Pairing

Complete capability is determined by pairing three Actions with list view base classes. `BaseListView` / `BaseDetailView` (provided by core addon, see [View Base Classes](#view-base-classes-viewbase-package)) already implement `DetailViewCloseCallback`; standard `StandardListView` doesn't implement this interface.

| Feature | StandardListView + Standard Action | StandardListView + MagicAction | BaseListView + Standard Action | BaseListView + MagicAction |
|---------|-----------------------------------|-------------------------------|------------------------------|---------------------------|
| **TAB container operation** | ✅ Standard | ✅ Standard | ✅ Standard (findFirstGrid fallback) | ✅ Standard |
| **TAB precise routing (multi-Grid)** | ❌ No `_pgrid` | ✅ `_pgrid` precise positioning | ❌ No `_pgrid`, uses findFirstGrid | ✅ `_pgrid` precise positioning |
| **TAB DetailViewCloseCallback** | — Not implemented | — Not implemented | ✅ Triggers (container may be null) | ✅ Triggers |
| **TAB afterSaveHandler** | ❌ No bridging | ✅ ComponentUtil bridging | ❌ No bridging | ✅ ComponentUtil bridging |
| **DIALOG container operation** | ✅ Jmix standard | ✅ Jmix standard | ✅ Jmix standard | ✅ Jmix standard |
| **DIALOG DetailViewCloseCallback** | — Not implemented | — Not implemented | ❌ Standard Action doesn't trigger callback | ✅ MagicAction triggers callback |
| **DIALOG scrollToIndex** | ❌ None | ❌ None | ❌ None | ✅ afterSaved default implementation |
| **DIALOG afterSaveHandler** | ✅ Standard support | ✅ Standard support | ✅ Standard support | ✅ Standard support |

- **Recommended combination**: `BaseListView` + MagicAction, for complete four-phase callbacks and precise routing.
- **Single-Grid page**: `BaseListView` + Standard Action's TAB mode also works (findFirstGrid fallback), but DIALOG mode `DetailViewCloseCallback` won't trigger.

---

## DetailViewCloseCallback - Detail Close Callback Contract

Interface unifying DetailView post-close callback timing, located at `org.magic.jmix.addons.core.view.base.DetailViewCloseCallback`.

### Background

Standard `list_create` / `list_edit` in DIALOG mode have inconsistent close callback timing, lack "close to interrupt" entry point. This interface defines four-phase timing, Phase 1-3 triggered by enhanced Actions (DIALOG mode) or multi-tab layout plugin's `TabDetailProcessor` (TAB mode), Phase 4 triggered by Action or `ComponentUtil` bridge.

### Interface Contract

```java
public interface DetailViewCloseCallback {

    /** Phase 1: Triggers on any close. Return false to interrupt, skip subsequent phases. */
    default boolean onClose(CloseAction closeAction,
                            @Nullable Object entity,
                            @Nullable CollectionContainer<?> container,
                            @Nullable ListDataComponent<?> sourceGrid,
                            boolean isNew) {
        return true;
    }

    /** Phase 2: Triggers only on SAVE, handles data logic. Return false to skip default container operation (e.g., MetaClass Grid needs own refreshAll). */
    default boolean onSaved(@Nullable Object entity,
                            @Nullable CollectionContainer<?> container,
                            @Nullable ListDataComponent<?> sourceGrid,
                            boolean isNew) {
        return true;
    }

    /** Phase 3: Triggers only on SAVE, handles UI logic. */
    default void afterSaved(@Nullable Object entity,
                            @Nullable ListDataComponent<?> sourceGrid,
                            boolean isNew) {
        // Default implementation: scroll Grid to top when creating new
        if (isNew && sourceGrid instanceof DataGrid<?> grid) {
            grid.scrollToIndex(0);
        }
    }
}
```

All methods are `default`, override as needed.

### Four-Phase Timing

| Phase | Method | Trigger Condition | Purpose | Return Value |
|-------|--------|-------------------|---------|--------------|
| Phase 1 | `onClose` | Any close | Interruptible entry, return false to fully take over | `boolean`: true continue, false interrupt |
| Phase 2 | `onSaved` | SAVE only | Data logic (refresh data source) | `boolean`: true use default container operation, false skip |
| Phase 3 | `afterSaved` | SAVE only | UI logic (scroll, select, focus) | void |
| Phase 4 | `afterSaveHandler` | SAVE only | Action-level whole ListView post-processing | — |

### Override Example

List view implements interface and overrides phases as needed. Example extending `BaseListView` (provided by core addon, base class already implements interface and provides default behavior):

```java
public class UserListView extends BaseListView<User> {

    // Phase 1: Close callback (triggers on any close)
    @Override
    public boolean onClose(CloseAction action, Object entity,
                           CollectionContainer<?> container,
                           ListDataComponent<?> grid, boolean isNew) {
        // Return true to continue subsequent phases, return false to fully take over close logic
        return true;
    }

    // Phase 2: Data logic callback (triggers only on SAVE)
    @Override
    public boolean onSaved(Object entity, CollectionContainer<?> container,
                           ListDataComponent<?> grid, boolean isNew) {
        // Return true to use default container operation (add/replace), return false to skip (e.g., MetaClass Grid)
        return true;
    }

    // Phase 3: UI logic callback (triggers only on SAVE)
    @Override
    public void afterSaved(Object entity, ListDataComponent<?> grid, boolean isNew) {
        // Default behavior: scroll Grid to top when creating new; can override for additional UI operations (e.g., select, focus)
    }
}
```

### TAB vs DIALOG Differences

| Phase | TAB Mode | DIALOG Mode |
|-------|----------|-------------|
| Phase 1: onClose | ✅ | ✅ |
| Phase 2: onSaved return true | defaultAddToContainer() | Skip (Jmix already did) |
| Phase 2: onSaved return false | Skip container operation | Skip |
| Phase 3: afterSaved | ✅ | ✅ |
| Phase 4: afterSaveHandler | ✅ (ComponentUtil bridge) | ✅ (Action direct call) |

### Common Override Patterns

| Scenario | onClose | onSaved | afterSaved |
|----------|---------|---------|------------|
| Default (BaseListView) | return true | return true | No override needed |
| MetaClass Grid | return true | return false (refreshAll in onSaved) | No override needed |
| Fully custom | return false | — | — |
| Skip container operation, UI only | return true | return false | Custom UI operation |

### Who Should Implement

- List views (`StandardListView`) needing unified close callback in DIALOG mode should implement this interface and override as needed;
- core's `BaseListView` / `BaseDetailView` already implement.

### Parameter Description

| Parameter | Description |
|-----------|-------------|
| `closeAction` | Close action (`StandardOutcome.SAVE` / `CLOSE` / `DISCARD`, etc.) |
| `entity` | Edited entity (null when not saved) |
| `container` | List data container (null for MetaClass Grid) |
| `sourceGrid` | Grid/List component triggering navigation (may be null) |
| `isNew` | Whether creating (CREATE is true, EDIT is false) |

---

## Multi-Tab Contracts (tab package) - Cross-addon Decoupling

"View lifecycle events + presentation mode/behavior declaration" contracts, located in `org.magic.jmix.addons.core.tab` package.

### Background / Why in core

Multi-tab layout plugin provides multi-tab architecture, but it's a **host framework-level** capability, other addons shouldn't directly depend on it. To allow any addon to declare "I enjoy multi-tab features" while staying loosely coupled, cross-addon shared contracts are placed in core:

- Users only depend on core, attach annotations / implement interfaces to declare multi-tab features;
- In plain hosts (multi-tab layout plugin not installed): Annotations ignored, events not triggered, but **no side effects, no errors**, views work normally;
- In hosts with multi-tab layout plugin installed: Automatically gain multi-tab enhancements (tab opening, activation/deactivation events, non-closeable/multi-open declarations, etc.).

### Contract List

| Class | Type | Description |
|-------|------|-------------|
| `TabActivationAware` | Interface | View `implements` then can listen to activation/deactivation events with `@Subscribe`. Provides `addTabActivateListener` / `addTabDeactivateListener` two default methods |
| `TabActivateEvent` | Event | Triggered when tab is activated (user switches to this tab) |
| `TabDeactivateEvent` | Event | Triggered when tab is deactivated (user switches away from this tab) |
| `PresentationMode` | Enum | Presentation mode: `TAB` / `DIALOG` / `PAGE` / `INITIAL` |
| `@PresentationModes` | Annotation | Declares supported presentation modes for view, first is default mode |
| `PresentationModeHelper` | Utility | Parses view's supported presentation modes; `isManagedByMainView()` checks if view is managed by MainView |
| `@UncloseableTab` | Annotation | Declares tab non-closeable (no close button) |
| `@MultipleOpenTab` | Annotation | Declares allowing multiple tab instances of same view |

### Usage Examples

#### Listening to Tab Activation/Deactivation

```java
import org.magic.jmix.addons.core.tab.TabActivationAware;
import org.magic.jmix.addons.core.tab.TabActivateEvent;
import org.magic.jmix.addons.core.tab.TabDeactivateEvent;

public class FileListView extends StandardListView<File> implements TabActivationAware {

    @Subscribe
    public void onTabActivate(TabActivateEvent event) {
        // Tab activated: refresh data, start polling
        refreshData();
        refreshTimer.start();
    }

    @Subscribe
    public void onTabDeactivate(TabDeactivateEvent event) {
        // Tab deactivated: stop polling, save temporary state
        refreshTimer.stop();
    }
}
```

> `TabActivationAware`'s two default methods are only for Jmix's `@Subscribe` reflection scan to match event types, no need to call manually. Matching mechanism is pure reflection, by event `Class` object, doesn't depend on package name.

#### Declaring Presentation Modes and Tab Behavior

```java
import org.magic.jmix.addons.core.tab.annotation.PresentationMode;
import org.magic.jmix.addons.core.tab.annotation.PresentationModes;
import org.magic.jmix.addons.core.tab.annotation.UncloseableTab;

@PresentationModes({PresentationMode.TAB, PresentationMode.DIALOG})
@UncloseableTab
public class FileListView extends StandardListView<File> implements TabActivationAware {
    // Supports TAB / DIALOG opening; tab non-closeable
}
```

### Event Trigger Mechanism

| Scenario | Behavior |
|----------|----------|
| Tab switch (multi-tab layout plugin installed) | `TabManager.activateTab()` first fires `TabDeactivateEvent` (old view), then fires `TabActivateEvent` (new view) |
| Tab close | Doesn't trigger activation/deactivation events, goes through standard `BeforeCloseEvent` / `AfterCloseEvent` |
| Plain host (multi-tab layout plugin not installed) | Events not triggered, but interface methods and annotations are harmless |

### Who Should Use

- **Addon views needing multi-tab features**: Depend on core, implement `TabActivationAware`, attach presentation mode/behavior annotations, don't感知 whether multi-tab layout plugin exists.
- **Host project views**: Same as above. If host installed multi-tab layout plugin, tab system's `TabManager` will read these contracts and provide enhancements.

> These contracts' **runtime implementation** (tab creation, activation, route interception, URL sync, etc.) is still in multi-tab layout plugin's `TabManager` / `TabRouterLayout` etc. core only provides contracts, not multi-tab runtime.

---

## Cross-View Navigation Contracts (view.navigation package) - Cross-View Data Passing Decoupling

Decoupling solution for cross-view data passing, located in `org.magic.jmix.addons.core.view.navigation` package.

### Background / Why in core

Under multi-tab architecture, Jmix's native `viewNavigators.view().withAfterNavigationHandler()` doesn't work (callback depends on origin view detach, but view doesn't detach under multi-tab). Core addon defines `ViewNavigationSupport` contract interface + default implementation (uses Jmix viewNavigators), other plugins can provide overriding implementations by implementing this interface. Users inject `ViewNavigationSupport` for cross-view data passing, gaining better extensibility through the contract interface.

- Users only depend on core, inject `ViewNavigationSupport` for cross-view data passing;
- In plain hosts: Uses default implementation (Jmix native `viewNavigators`);
- Other plugins can provide overriding implementations via `@ConditionalOnMissingBean` mechanism.

### Contract List

| Class | Type | Description |
|-------|------|-------------|
| `ViewNavigationSupport` | Interface | Contract interface, defines `open(viewClass)` method, returns `ViewNavigationBuilder<V>` |
| `ViewNavigationBuilder<V>` | Builder base class | Provides `withAfterViewCreated` / `withAfterViewClosed` / `navigate` fluent API |
| `AfterViewClosedEvent<V>` | Event | Close event, provides `getView()` / `getCloseAction()` / `closedWith(StandardOutcome)` |
| `DefaultViewNavigationSupport` | Default implementation | Uses Jmix native `viewNavigators`, registered with `@ConditionalOnMissingBean` |

### Usage

```java
import org.magic.jmix.addons.core.view.navigation.ViewNavigationSupport;
import org.magic.jmix.addons.core.view.navigation.AfterViewClosedEvent;

@Autowired
private ViewNavigationSupport viewNavigationSupport;

// Open view and pass initialization parameters
viewNavigationSupport.open(MyView.class)
    .withAfterViewCreated(view -> view.setData(data))
    .navigate();

// Open view and receive close result
viewNavigationSupport.open(UserDetailView.class)
    .withAfterViewCreated(view -> view.setEntityToEdit(user))
    .withAfterViewClosed(event -> {
        if (event.closedWith(StandardOutcome.SAVE)) {
            collectionDc.replaceItem(event.getView().getEditedEntity());
        }
    })
    .navigate();
```

### Bean Override Mechanism

| Scenario | Injected Implementation | Description |
|----------|------------------------|-------------|
| No override | `DefaultViewNavigationSupport` | Uses Jmix native `viewNavigators` |
| Override exists | Override implementation class | Provided by other plugins via `@ConditionalOnMissingBean` |

### ViewNavigationBuilder API

| Method | Description |
|--------|-------------|
| `withAfterViewCreated(Consumer<V>)` | One-time callback, triggered after view creation, before BeforeShow |
| `withAfterViewClosed(Consumer<AfterViewClosedEvent<V>>)` | One-time close callback, triggered when target view's AfterCloseEvent fires |
| `navigate()` | Execute navigation, trigger route |

### AfterViewClosedEvent Properties

| Method | Description |
|--------|-------------|
| `getView()` | Closed view instance |
| `getCloseAction()` | Close action, consistent with `View.AfterCloseEvent.getCloseAction()` |
| `closedWith(StandardOutcome)` | Check if view closed with specified outcome |

### Limitations

`viewNavigationSupport.open()` only supports parameterless routes (e.g., `@Route("users")`), does not support routes with path parameters (e.g., `@Route("user/:id")`). For parameterized routes, use `viewNavigators.detailView().navigate()`.

### Who Should Use

- **Views needing cross-view data passing under multi-tab architecture**: **Must** use `ViewNavigationSupport`. Jmix's native `viewNavigators.view().withAfterNavigationHandler()` doesn't work under multi-tab architecture, making `ViewNavigationSupport` the only reliable way to pass data between views.
- **Views not under multi-tab architecture**: Also recommended to use `ViewNavigationSupport`. The default implementation uses Jmix viewNavigators with equivalent functionality; plus you gain extensibility through the contract interface — when multi-tab layout plugin is installed later, no code changes needed to automatically get enhancements.

> This contract's **runtime implementation** (callback capabilities under multi-tab architecture, etc.) is provided by other plugins. Core only provides the contract and default implementation, not multi-tab architecture runtime.

---

## View Base Classes (view.base package)

List/detail view base classes, located in `org.magic.jmix.addons.core.view.base`.

### BaseListView

List view base class, extends `StandardListView<T>`, implements `DetailViewCloseCallback` + `TabActivationAware`:

- Provides default implementations for [DetailViewCloseCallback](#detailviewclosecallback---detail-close-callback-contract) four-phase close callbacks (`onClose` / `onSaved` / `afterSaved`), override as needed
- Built-in `TabActivationAware`, can `@Subscribe` listen to `TabActivateEvent` / `TabDeactivateEvent`
- `loadAllCollectionLoaders()` manually triggers all CollectionLoaders in view to load

After extending and pairing with enhanced Actions (`magic_list_create` / `magic_list_edit`), automatically get smart data refresh (new items at top / edit refresh single row).

### BaseDetailView

Detail view base class, extends `StandardDetailView<T>`, implements `TabActivationAware`:

- Auto title: New shows "New:EntityName", Edit shows "Edit:EntityName:InstanceName", View shows "View:EntityName:InstanceName"
- Save state tracking: `isDataSaved()` / `isNewEntity()` / `getSavedEntity()` / `isSavedNewEntity()`
- Built-in `TabActivationAware`

### Auto Title Messages

BaseDetailView's title prefix messages are provided by core, can override in project message files:

| Message Key | Chinese | English |
|-------------|---------|---------|
| `org.magic.jmix.addons.core/detailView.title.new` | 新建 | New |
| `org.magic.jmix.addons.core/detailView.title.edit` | 编辑 | Edit |
| `org.magic.jmix.addons.core/detailView.title.view` | 查看 | View |

> `BaseListView` / `BaseDetailView` paired with multi-tab event contracts, `DetailViewCloseCallback`, get complete TAB mode CRUD timing in hosts with multi-tab layout plugin installed; work as standard view base classes in plain hosts.

---

## Jmix Framework Translation Supplement

Supplements untranslated Chinese messages in Jmix 2.8 framework, automatically effective after installing core addon.

### Supplemented Translations

| Message Key | Chinese |
|-------------|---------|
| `pagination.itemsPerPage.emptySelectionCaption` | 最大 |
| `actions.genericFilter.Edit` | 编辑 |
| `actions.genericFilter.Reset` | <重置过滤> |
| `genericFilter.emptyConfiguration.name` | 过滤 |
| `genericFilter.addConditionButton.text` | 添加过滤条件 |
| `connectionIndicator.online` | 在线 |
| `connectionIndicator.offline` | 连接已断开 |
| `connectionIndicator.reconnecting` | 连接已断开，正在尝试重连… |

---

## Version Compatibility

| Jmix Version | Vaadin Version | Plugin Version |
|--------------|----------------|----------------|
| 2.8.1+ | 24.x | 0.0.2 |

## Changelog

### 0.0.1

- Added BaseEntity entity base class (audit fields + UUID primary key)
- Added AppConfig application configuration entity
- Added AppConfigService configuration service
- Added enhanced UI components: MagicGenericFilter, MagicSimplePagination (auto-replace standard components)
- Added LazyContainerDataGridItems lazy loading DataGrid data provider
- Added GenericUniqueConstraintViolationHandler unique constraint violation handler
- Added TreeGridScrollHelper utility class
- Added NotificationUtil unified notification utility (`magic.core.*` config, position two-level override, title i18n, hot-modify compatible)
- Added MagicDetailWindowBuilderProcessor: `@Primary` replaces framework `DetailWindowBuilderProcessor`, fixes Dialog mode dirty data backwrite to list when not persisted
- Added enhanced Actions: `magic_list_create` / `magic_list_edit` / `magic_list_remove`, unify TAB/DIALOG close callback timing, compatible with Container/MetaClass Grid modes
- Added DetailViewCloseCallback detail close callback contract interface (four phases: onClose/onSaved/afterSaved/afterSaveHandler)
- Added multi-tab contracts (`tab` package): `TabActivationAware` / `TabActivateEvent` / `TabDeactivateEvent` (tab activation/deactivation event contracts), `@PresentationModes` / `PresentationMode` / `PresentationModeHelper` (presentation mode declaration), `@UncloseableTab` / `@MultipleOpenTab` (tab behavior declaration). Allow other addons to declare multi-tab features by only depending on core, no side effects in plain hosts, automatically gain enhancements after installing multi-tab layout plugin.
- Added view base classes `BaseListView` / `BaseDetailView` (`org.magic.jmix.addons.core.view.base`). `BaseListView` provides `DetailViewCloseCallback` four-phase close callback default implementations; `BaseDetailView` provides auto title and save state tracking; auto title messages `detailView.title.new/edit/view` also provided.
- Added `CursorLazyGrid` cursor pagination lazy loading Grid component (`CursorLazyGrid` / `CursorPagedDataProvider` / `CursorPage` / `CursorPageFetcher`), adapting cursor sequential pagination backend to Grid native scroll lazy loading, supports undefined size mode and async `enablePushUpdates`; note its DataProvider is **fully cached** (only grows), evaluate memory for large data scenarios.

### 0.0.2

- Added cross-view navigation contracts (`view.navigation` package): `ViewNavigationSupport` contract interface + `ViewNavigationBuilder<V>` builder base class + `AfterViewClosedEvent<V>` close event + `DefaultViewNavigationSupport` default implementation. Solves the issue where `withAfterNavigationHandler` fails under multi-tab architecture; other plugins can provide overriding implementations via `@ConditionalOnMissingBean` mechanism.
