# A small settings page using the existing components

The sample app already hosts `PreferencesFragment` inside `DrawerLayout` in `activity_main.xml`. A settings page can use that same host and the existing preference classes. The page reads like this:

```text
Settings
  Appearance
    Follow system theme                 [switch]
    Blur effects                        [switch]
    Accent colour                       [colour picker]
  Controls
    Animation speed                     [slider]
    Default view                        [choice]
```

Add a preference XML resource, for example `res/xml/settings_page.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <!-- Categories give the page short, scannable groups. -->
    <PreferenceCategory android:title="Appearance">
        <!-- A standard saved switch. The preference framework persists it. -->
        <SwitchPreferenceCompat
            android:key="follow_system_theme"
            android:title="Follow system theme"
            android:defaultValue="true" />

        <!-- BlurSettings reads this exact key from default shared preferences. -->
        <SwitchPreferenceCompat
            android:key="global_blur_enabled"
            android:title="Blur effects"
            android:summary="Blur floating bars, search and dialogs"
            android:defaultValue="true" />

        <!-- Reuse the existing One UI colour dialog; no custom picker screen. -->
        <dev.oneuiproject.oneui.preference.ColorPickerPreference
            android:key="accent_colour"
            android:title="Accent colour"
            android:defaultValue="#0381FE" />
    </PreferenceCategory>

    <PreferenceCategory android:title="Controls">
        <!-- The library handles the slider, value persistence, and touch UI. -->
        <dev.oneuiproject.oneui.preference.OneUISliderPreference
            android:key="animation_speed"
            android:title="Animation speed"
            android:summary="Choose how quickly controls move"
            android:defaultValue="3"
            app:minValue="0"
            app:maxValue="5"
            app:stepSize="1"
            app:showTicks="true" />

        <!-- A standard list is simplest for a short, mutually exclusive choice. -->
        <ListPreference
            android:key="default_view"
            android:title="Default view"
            android:entries="@array/settings_view_labels"
            android:entryValues="@array/settings_view_values"
            android:defaultValue="list"
            app:useSimpleSummaryProvider="true" />
    </PreferenceCategory>
</PreferenceScreen>
```

Define the two list arrays in `res/values/arrays.xml`:

```xml
<string-array name="settings_view_labels">
    <item>List</item>
    <item>Grid</item>
</string-array>
<string-array name="settings_view_values">
    <item>list</item>
    <item>grid</item>
</string-array>
```

Then load it with the same pattern as the existing `PreferencesFragment`:

```java
public final class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle state, String rootKey) {
        // This builds the rows, restores saved values, and handles persistence.
        setPreferencesFromResource(R.xml.settings_page, rootKey);
    }
}
```

This is the easiest starting point because the sample already has the `DrawerLayout` host and a `PreferencesFragment`. Preference rows own their saved values and accessibility behaviour. Add a listener only where a value must immediately change app behaviour, such as applying the selected accent colour. The blur switch needs no custom listener because `BlurSettings` observes the shared key.

## How the blur setting works

`BlurSettings.KEY` is `global_blur_enabled` in default shared preferences. It defaults to `true`. A component blurs only when both the global setting and that component's own blur option are enabled. For example, `layout.setStickyActionBlurEnabled(true)` allows the top actions to blur, while `BlurSettings.setEnabled(context, false)` turns all participating blur effects off. The preference switch above writes the same key, so attached components redraw when it changes.

`FloatingSearchBar` uses `GlassSurfaceView` to sample the view supplied by `setSourceView`. It refreshes that sample while visible, so changing content behind the pill stays current. The sample app passes its scroll content and switches to the result list when a query is active. `FloatingNavigationBar`, `FloatingToolbar`, top action surfaces, and dialogs use the same global gate. Dialog window blur is available on Android 12+; earlier versions keep the translucent surface. `ProgressiveBlurLayout` and `ScrollEdgeFades` are colour fades at scroll edges rather than pixel blur, but the global switch gates those too.
