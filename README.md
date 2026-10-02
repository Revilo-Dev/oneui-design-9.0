<p align="center">
  <img loading="lazy" src="readme-res/design-readme-header.png"/>
  <br><br>
  <a href="https://t.me/oneuiproject"><img src="https://img.shields.io/badge/Telegram-OneUI_Project-blue.svg?style=for-the-badge&logo=Telegram"/></a>
  <a href="https://mvnrepository.com/artifact/io.github.oneuiproject/design"><img src="https://img.shields.io/maven-central/v/io.github.oneuiproject/design?color=%23C71A36&label=Maven&logo=Apache%20Maven&logoColor=%23C11920&style=for-the-badge"/></a>
  <h3 align="center"><a href="https://github.com/OneUIProject/oneui-design/raw/main/sample-app/release/sample-app-release.apk">Download Sample APK</a></h3>
</p>

<h1></h1>

In this repository you'll find:
- Source code of the [oneui-design library](#oneui-design-module)
- Source code of our [One UI Sample app](#oneui-sample-app) (see [Kotlin version](https://github.com/Lemkinator/OneUI-Sample-App)

Any form of contribution, suggestions, bug report or feature request will be welcome.

# OneUI Design Module

The "oneui-design" library (not to be confused with the old [OneUI Design Library](https://github.com/OneUIProject/OneUI-Design-Library)) consists of custom components made to help you implement One UI in your apps with ease, if you're actually interested on the base libraries please take a look at [oneui-core](https://github.com/OneUIProject/oneui-core). Check our [documentation page](https://oneuiproject.github.io/design/) to learn more about this module.

## Usage

- Make sure your Android project has [oneui-core](https://github.com/OneUIProject/oneui-core#usage) libraries implemented;

- Add the design library in your dependencies:
```groovy
dependencies {
    // ...
    implementation 'io.github.oneuiproject:design:<version>'
}
```

- Apply the main theme in your AndroidManifest file:
```xml
<application
    ...
    android:theme="@style/OneUITheme" >
    ...
</application>
```

# OneUI Sample App

<p align="center"><img loading="lazy" src="readme-res/sample-ss1.jpg" height="280"/> <img loading="lazy" src="readme-res/sample-ss2.jpg" height="280"/> <img loading="lazy" src="readme-res/sample-ss3.jpg" height="280"/> <img loading="lazy" src="readme-res/sample-ss4.jpg" height="280"/> <img loading="lazy" src="readme-res/sample-ss5.jpg" height="280"/></p>

The One UI Sample app has been made to showcase the components from both our [oneui-core](https://github.com/OneUIProject/oneui-core) libraries and [oneui-design](#oneui-design-module) module. You can download the latest apk [here](https://github.com/OneUIProject/oneui-design/raw/main/sample-app/release/sample-app-release.apk), for the older versions of the app please check the deprecated [OneUI Design Library](https://github.com/OneUIProject/OneUI-Design-Library) repository.

## Features
- Supports Android 6.0 (api 23) and above;
- Supports both Samsung and non-Samsung devices;
- Includes `ProgressiveBlurLayout` for fading blur at the top and bottom of scrollable content (API 23+);
- Includes `FloatingNavigationBar` for a floating navigation pill with optional labels, backdrop blur, and a separate end action;
- Includes `OneUISlider` and `FloatingToolbar` for the thicker slider treatment and compact contextual actions;
- One UI 4 Color Theme support;
- Example UI for the following components:
  - AppCompat (Base widgets/theme);
  - Material Components ([Tabs](https://material.io/components/tabs), [Bottom navigation](https://material.io/components/bottom-navigation));
  - Date/Time/Color pickers;
  - ListViews (AppPicker, Contacts, Icons);
  - Preferences;
  - Swipe to Refresh;

# Scroll edge fades

The Flow-style progressive effect is a themed color fade, not a pixel blur. `ToolbarLayout` and `DrawerLayout` install it on every scrollable page descendant, including `RecyclerView`, `ListView`, `ScrollView`, and `NestedScrollView`. It appears only when more content exists in that direction and never consumes touch events. To use it outside those layouts, call `ScrollEdgeFades.attach(scrollable)` or wrap a single scrolling view in `ProgressiveBlurLayout`. Both default to 56dp edges. The existing `blurHeight`, `blurTop`, and `blurBottom` XML names remain supported by the wrapper.

```xml
<dev.oneuiproject.oneui.widget.ProgressiveBlurLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:blurHeight="56dp">

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</dev.oneuiproject.oneui.widget.ProgressiveBlurLayout>
```

Use `setFadeHeights(topPx, bottomPx)`, `setBlurEdges(top, bottom)`, and `setFadeColor(color)` on the wrapper. Both fade components use the nearest solid surface color by default. `ScrollEdgeFades.attach(view)` returns a controller with `setHeights`, `setEdges`, `setColor`, and `detach`. Use `attachTree(root, surfaceColor)` for translucent panels such as a drawer. A second `attach` call returns the existing controller.

## Pinned action bar and floating search

`ToolbarLayout` pins its toolbar when the large header collapses. Once page content scrolls behind it, the actual navigation button and action menu become rounded control groups. The action group measures around its visible icons, with equal icon widths and padding. The toolbar remains transparent. The compact page title appears when the header closes and leaves as the content scrolls; enable `setStickyTitleEnabled(true)` to keep it pinned. Screens can add actions through `getToolbar()` and configure the groups through `setStickyActionsEnabled`, `setStickyActionBlurEnabled`, `setStickyActionShadowElevation`, and `setStickyActionTintColor`. `getStickyToolbarControls()` also exposes icon width, group padding, corner radius, horizontal inset, collapse tolerance, and content scroll trigger options. A standalone `CoordinatorLayout` can use `StickyToolbarControls(appBar, toolbar, content)`, as the About screen does.

`showSearchMode()` opens the 56dp bottom search pill above the navigation bar or keyboard. Its close button and Back clear the query and restore the page. `showSearchMode(initialQuery)` restores a query without sending a duplicate text callback. Screens own filtering through `setSearchModeListener`; use `getFloatingSearchBar()` to customize the hint, icons, animation timing, blur surface, and input field. Opening search hides the sticky bottom bar and footer, then restores them on close.

```java
ToolbarLayout layout = findViewById(R.id.toolbar_layout);
layout.getToolbar().inflateMenu(R.menu.my_actions);
layout.getFloatingSearchBar().setHint("Search items");
layout.getFloatingSearchBar().setAnimationDurations(180, 130);
layout.getStickyToolbarControls().setCornerRadius(
        Math.round(28 * layout.getResources().getDisplayMetrics().density));
layout.getStickyToolbarControls().setItemWidth(
        Math.round(48 * layout.getResources().getDisplayMetrics().density));
layout.setStickyActionBlurEnabled(true);
```

The sample's search action filters its page list as text changes. For application data, connect `SearchModeListener.onQueryTextChange` to the page's adapter and clear that filter when the callback receives an empty string.

## Floating navigation

`FloatingNavigationBar` is a reusable view for compact navigation. Add it over content in a `FrameLayout`, leaving a bottom content inset so the last row stays reachable. Its menu items provide stable IDs, icons, and accessible titles. Use up to five destinations, with an optional circular end action independent of navigation selection.

```xml
<dev.oneuiproject.oneui.widget.FloatingNavigationBar
    android:id="@+id/floating_nav"
    android:layout_width="wrap_content"
    android:layout_height="58dp"
    android:layout_gravity="bottom|center_horizontal"
    android:layout_margin="16dp"
    app:navigationMenu="@menu/main_destinations"
    app:showLabels="true"
    app:blurEnabled="true"
    app:actionIcon="@drawable/ic_add"
    app:actionContentDescription="Add item" />
```

```java
FloatingNavigationBar nav = findViewById(R.id.floating_nav);
nav.setOnItemSelectedListener(itemId -> showDestination(itemId));
nav.setAction(R.drawable.ic_add, "Add item", v -> addItem());
// Optional: nav.setShowLabels(false); nav.setShowIcons(false);
// nav.setBlurEnabled(false); nav.setAnimationsEnabled(false); nav.clearAction();
// nav.setItemWidth(64dp); nav.setBarHeight(58dp); nav.setSelectedColor(color);
```

To link actual pages, put them as direct children of `NavigationPageContainer`. Give each page the same ID as its menu item, then bind once:

```java
FloatingNavigationBar nav = findViewById(R.id.floating_nav);
NavigationPageContainer pages = findViewById(R.id.pages);
nav.bindPages(pages);
nav.setPageSwipingEnabled(true); // Optional; false disables horizontal page swipes.
```

Place the page container and bar as siblings in a full-height `FrameLayout`, with the bar aligned to the bottom. Leave 88dp of bottom space in the page container so content stays reachable. A menu item without a matching page can be handled by `setOnItemSelectedListener`, as the sample does for its popup. The sample has Grid, List, Cards, and Settings pages; the explanatory card is only on Settings. Press and page transitions follow `setAnimationsEnabled`. With blur enabled, the view samples content behind it; solid mode skips this work. Surfaces use `#E3E3E3` in light mode and `#2E2E30` in dark mode, with `#FFFFFF` and `#3D3D3D` selected pills. Samsung's [bottom navigation guidance](https://developer.samsung.com/one-ui/comp/bottom-navigation.html) describes the tab behavior and [bottom bar guidance](https://developer.samsung.com/one-ui/comp/bottom-bar.html) describes bottom actions.

For one to three destinations without an end action, the bar sizes itself to its items. Use `android:layout_gravity="bottom|center_horizontal"` inside a `FrameLayout` to center a compact bar. Four or five destinations fill the available width.

## Sliders

`OneUISlider` supports a thick rounded track, a palette-colored active segment, and a circular thumb filled with the current background color. It works from API 23, supports touch, keyboard and accessibility adjustments, and can show discrete steps, vertical orientation, haptic steps, and a warning threshold. The sample's stepped sliders have no fill.

```xml
<dev.oneuiproject.oneui.widget.OneUISlider
    android:id="@+id/volume_slider"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:contentDescription="Volume"
    app:minValue="0"
    app:maxValue="100"
    app:sliderValue="50"
    app:leadingIcon="@drawable/ic_volume" />
```

Use `app:stepSize` with `app:showTicks="true"` for fixed levels and `app:sliderFillEnabled="false"` for a track without fill. `app:sliderVertical`, `app:sliderHapticEnabled`, and `app:sliderWarningValue` enable the other variants. `setOnValueChangeListener` reports the new value and whether it came from the user. Active, track, and thumb colors can be overridden per view in XML.

`OneUISliderPreference` puts the same slider in a persistent preference row. Set a unique `android:key`, `android:defaultValue`, and the same `app:minValue`, `app:maxValue`, `app:stepSize`, `app:showTicks`, and `app:sliderFillEnabled` attributes used by the view. Preference change listeners can accept or reject user changes.

The active segment uses the theme's `colorPrimary` by default. Use `app:sliderActiveColor` or `setActiveColor` to override it for one slider.

## Floating toolbar

`FloatingToolbar` displays up to five independent actions from an Android menu resource. Each visible item needs an ID and title; an icon is optional. The title remains the accessibility label when visual labels are hidden. Use `app:toolbarLightSurface` to choose the light or dark pill, `app:toolbarShowLabels` to control captions, and `setBlurEnabled` for a glass surface. `FloatingActionBar` is a two-command Cancel/Save bar with configurable labels and callbacks. Both use the same light/dark surface colors as navigation.

```xml
<dev.oneuiproject.oneui.widget.FloatingToolbar
    android:id="@+id/selection_toolbar"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    app:toolbarMenu="@menu/selection_actions"
    app:toolbarLightSurface="false" />
```

```java
FloatingToolbar toolbar = findViewById(R.id.selection_toolbar);
toolbar.setOnActionClickListener(actionId -> handleAction(actionId));
toolbar.setBlurEnabled(true);
```

`FloatingBarSwitcher` keeps a navigation bar and one or more contextual bars in a single sticky bottom slot. Add the bars as direct children, call `setNavigationBar(nav)`, then `showBar(toolbar)` or `showNavigation()`. Set `setBackdropView(pageContainer)` when blurred bars should sample only the page behind them. `setAnimationsEnabled(false)` makes the swap immediate. The sample Toolbar page has a Toolbar tab and an Action bar tab, each with an open button. Samsung's [bottom bar guidance](https://developer.samsung.com/one-ui/comp/bottom-bar.html) recommends up to five bottom actions with icons and text; the floating presentation follows the supplied reference.

When a page uses `ToolbarLayout` or `DrawerLayout`, call `setStickyBottomBar(bar)` after its view is created. This moves the bar into a fixed overlay above the scrolling page; call `setStickyBottomBar(null)` when leaving the page. The sample does this when switching between Navigation and Toolbar. `GlassSurfaceView` provides the shared rounded blur surface used by the collapsed app bar and floating drawer. The expanded app bar retains its flat presentation. The theme gives AppCompat alert dialogs a rounded translucent surface and uses Android window blur on API 31+; older versions retain the translucent surface.

`BlurSettings` provides an app-wide override for glass surfaces, dialog backdrops, and progressive scroll fades. The sample exposes its `global_blur_enabled` key in Preferences. A component's own blur option still controls that component when the global setting is on. Call `BlurSettings.setEnabled(context, false)` to switch all blur off in code.

`FloatingSearchBar` can be embedded as a full search field with `setPersistent(true)`. Set its scroll source with `setSourceView`, supply a `Listener` for filtering or submission, and customize its hint, icons, and animation durations. The sample Widgets page uses the same class as the toolbar search overlay.

## Android 17 / One UI modernization plan

The build now uses Gradle 9.5.1, Android Gradle Plugin 9.3, and `compileSdk` 36. The installed SDK on the development machine stops at API 36.1, so API 37 compilation still requires installing the Android 17 SDK. The sample still targets API 31 and the project still uses older SESL dependencies. `ui9-v1.0` identifies the first migration release and does **not** claim that the whole library has been migrated yet.

1. Review available SESL releases and upgrade dependencies together, checking API compatibility. Keep `minSdk 23` until a dependency or feature forces a change; gate new platform APIs by SDK level. The legacy picker artifacts share a namespace and currently require the `android.uniquePackageNames=false` migration setting.
2. Install the Android 17 SDK and raise `compileSdk` to 37. Update the sample's `targetSdk` separately after testing Android 15+ edge-to-edge, window insets, predictive back, permissions, and Android 17 behavior changes.
3. Audit theme colors, typography, spacing, shapes, iconography, motion, dark mode, contrast, and touch targets against the current [Samsung One UI guidance](https://developer.samsung.com/one-ui/index.html). Remove reliance on undocumented Samsung internals where possible so non-Samsung devices remain supported.
4. Make layouts adaptive for tablets, foldables, landscape, split screen, and large font sizes. Add device and API-level visual checks, especially API 23, 31, 35, and 37.
5. Update publishing coordinates, README links, release artifacts, changelog, sample screens, and documentation for this fork before publishing a release.

Candidate library components: edge-to-edge and inset-aware screen scaffolds; adaptive navigation and responsive panes; current One UI color and typography tokens; bottom action bars and sheets; updated search and list patterns; accessible loading, empty, and error states; motion and haptic helpers; and a foldable-aware layout helper. These are proposals for implementation and visual review, not existing APIs.

# More info
- [One UI 4](https://design.samsung.com/global/contents/one-ui-4/)
- [One UI Design Guide](https://developer.samsung.com/one-ui/index.html)
- [One UI Design Guide (PDF)](https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf)

# Special thanks
- [Google](https://developer.android.com/jetpack) for their Jetpack and Material Components libraries.
- [Samsung](https://www.samsung.com/) for their awesome OneUI Design. :)
- All the current and future [contributors](https://github.com/OneUIProject/oneui-design/graphs/contributors) and issue reporters. :D
