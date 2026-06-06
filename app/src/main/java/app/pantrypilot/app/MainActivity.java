package app.pantrypilot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_SCAN_IMAGE = 701;
    private static final int REQ_SCAN_CAMERA = 702;
    private static final int BG = 0xffe8edf5;
    private static final int PANEL = 0xffffffff;
    private static final int TEXT = 0xff18212f;
    private static final int MUTED = 0xff687386;
    private static final int GREEN = 0xffe85d75;
    private static final int GREEN_DARK = 0xff3867d6;
    private static final int LINE = 0xffc9d3e3;
    private static final String PREFS = "pantry_pilot";
    private static final String PREF_PLAN = "plan";
    private static final String PREF_DEMO_PURCHASED_PLAN = "demo_purchased_plan";
    private static final String PREF_AD_FREE = "ad_free";
    private static final String PREF_DEMO_PURCHASED_AD_FREE = "demo_purchased_ad_free";
    private static final String PREF_TUTORIAL_SEEN = "tutorial_seen";
    private static final String BILLING_MODE_DEMO = "demo";
    private static final String BILLING_MODE_PRODUCTION = "production";
    private static final String BILLING_MODE = BILLING_MODE_DEMO;

    private SharedPreferences prefs;
    private LinearLayout content;
    private ScrollView currentScroll;
    private TextView status;
    private TextView summary;
    private int pendingScrollY = 0;
    private int activeTab = 0;
    private String currentStatus = "Ready.";
    private Bitmap scannerBitmap;
    private String scanNameDraft = "";
    private String scanQuantityDraft = "1";
    private String scanExpiryDraft = "";
    private String scanLocationDraft = "Pantry";
    private String scanCategoryDraft = "Other";
    private String scanLabelTextDraft = "";
    private Uri pendingCameraUri;
    private String currentPlan = PantryRules.PLAN_FREE;
    private ConsentInformation consentInformation;
    private boolean mobileAdsInitializing = false;
    private boolean mobileAdsReady = false;
    private FrameLayout currentAdHost;
    private TextView currentAdStatus;
    private Button currentPrivacyOptions;
    private AdView currentAdView;
    private final List<PantryItem> pantry = new ArrayList<>();
    private final List<ShopItem> shopping = new ArrayList<>();
    private final Set<String> expandedHome = new HashSet<>();

    private static class PantryItem {
        String name;
        String location;
        String quantity;
        String category;
        String expiry;

        PantryItem(String name, String location, String quantity, String category, String expiry) {
            this.name = name;
            this.location = location;
            this.quantity = quantity;
            this.category = category;
            this.expiry = expiry;
        }
    }

    private static class ShopItem {
        String name;
        boolean done;

        ShopItem(String name, boolean done) {
            this.name = name;
            this.done = done;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        currentPlan = PantryRules.normalizePlan(prefs.getString(PREF_PLAN, PantryRules.PLAN_FREE));
        loadData();
        if (pantry.isEmpty() && shopping.isEmpty()) seedDemoData();
        expandedHome.add("next");
        buildUi();
        requestAdConsent();
        if (!prefs.getBoolean(PREF_TUTORIAL_SEEN, false)) {
            currentScroll.postDelayed(() -> showTutorialStep(0), 350);
        }
    }

    private void buildUi() {
        destroyCurrentAdView();
        currentAdHost = null;
        currentAdStatus = null;
        currentPrivacyOptions = null;
        ScrollView scroll = new ScrollView(this);
        currentScroll = scroll;
        scroll.setBackgroundColor(BG);
        if (pendingScrollY > 0) {
            scroll.setVisibility(View.INVISIBLE);
        }
        LinearLayout root = column();
        int left = dp(14), top = dp(14), right = dp(14), bottom = dp(24);
        root.setPadding(left, top, right, bottom);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(left + insets.getSystemWindowInsetLeft(),
                    top + insets.getSystemWindowInsetTop(),
                    right + insets.getSystemWindowInsetRight(),
                    bottom + insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(root);

        LinearLayout header = row();
        LinearLayout titles = column();
        titles.addView(title("PantryPilot"));
        titles.addView(small("Your smart pantry pal for scanning, expiry, grocery, and meals"));
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        Button privacy = secondary("Privacy");
        Button about = secondary("About");
        header.addView(privacy);
        header.addView(about);
        root.addView(header);

        status = small(currentStatus);
        status.setPadding(dp(10), dp(10), dp(10), dp(10));
        status.setBackground(rounded(0xfffff4f6, 0xffffccd5, 8));
        status.setElevation(dp(2));
        root.addView(status, matchWrap());

        summary = small("");
        summary.setPadding(dp(12), dp(12), dp(12), dp(12));
        summary.setBackground(rounded(0xffeef7fb, 0xffc6e8f0, 8));
        summary.setElevation(dp(2));
        root.addView(summary, matchWrap());

        LinearLayout tabsWrap = column();
        LinearLayout tabs = null;
        String[] labels = {"Home", "Scan", "Pantry", "Grocery", "Meals", "Plans"};
        for (int i = 0; i < labels.length; i++) {
            if (i % 3 == 0) {
                tabs = row();
                tabsWrap.addView(tabs, matchWrap());
            }
            int index = i;
            Button tab = i == activeTab ? button(labels[i]) : secondary(labels[i]);
            tab.setSingleLine(true);
            tab.setEllipsize(TextUtils.TruncateAt.END);
            tab.setTextSize(14);
            tab.setMinWidth(0);
            tab.setMinimumWidth(0);
            tab.setMinEms(0);
            tab.setPadding(dp(2), 0, dp(2), 0);
            tab.setOnClickListener(v -> {
                activeTab = index;
                buildUi();
            });
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, -2, 1);
            tabParams.setMargins(0, 0, i % 3 == 2 ? 0 : dp(4), 0);
            tabs.addView(tab, tabParams);
        }
        root.addView(tabsWrap, matchWrap());

        View ad = freeAdBanner();
        if (ad != null) root.addView(ad, matchWrap());

        content = column();
        root.addView(content);

        privacy.setOnClickListener(v -> info(getString(R.string.privacy_title), getString(R.string.privacy_body)));
        about.setOnClickListener(v -> info(getString(R.string.about_title), getString(R.string.about_body)));

        renderActiveTab();
        setContentView(scroll);
        if (pendingScrollY > 0) {
            int restoreY = pendingScrollY;
            pendingScrollY = 0;
            scroll.post(() -> {
                scroll.scrollTo(0, restoreY);
                scroll.setVisibility(View.VISIBLE);
            });
        }
    }

    private void renderActiveTab() {
        updateSummary();
        content.removeAllViews();
        if (activeTab == 0) renderHome();
        if (activeTab == 1) renderScanner();
        if (activeTab == 2) renderPantry();
        if (activeTab == 3) renderGrocery();
        if (activeTab == 4) renderMeals();
        if (activeTab == 5) renderPlans();
    }

    private void renderHome() {
        content.addView(heroCard());
        content.addView(planStatusCard());
        LinearLayout grid = column();
        LinearLayout row1 = row();
        row1.addView(statCard("Pantry items", String.valueOf(pantry.size()), "Track what is already paid for"), new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(statCard("Expiring soon", String.valueOf(expiringCount()), "Use these first"), new LinearLayout.LayoutParams(0, -2, 1));
        grid.addView(row1);
        LinearLayout row2 = row();
        row2.addView(statCard("Grocery items", String.valueOf(openShoppingCount()), "Open grocery list items"), new LinearLayout.LayoutParams(0, -2, 1));
        row2.addView(statCard("Storage zones", String.valueOf(usedLocationCount()), "Fridge, freezer, pantry, counter"), new LinearLayout.LayoutParams(0, -2, 1));
        grid.addView(row2);
        content.addView(grid);

        LinearLayout next = column();
        Button scan = button("Scan item");
        Button addPantry = secondary("Add pantry item");
        LinearLayout nextActions = row();
        nextActions.addView(scan, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(0, -2, 1);
        addLp.setMargins(dp(8), 0, 0, 0);
        nextActions.addView(addPantry, addLp);
        next.addView(nextActions);
        next.addView(small("Use Scan for photo-assisted pantry item entry, or add a pantry item manually when the label is not worth photographing."));
        scan.setOnClickListener(v -> { activeTab = 1; buildUi(); });
        addPantry.setOnClickListener(v -> { activeTab = 2; buildUi(); });
        content.addView(collapsibleSection("next", "Next Action", "Scan or add pantry items without clutter", next));

        LinearLayout expiring = column();
        boolean anyExpiring = false;
        for (PantryItem item : pantry) {
            Long days = daysUntil(item.expiry);
            if (days != null && days <= 7) {
                anyExpiring = true;
                expiring.addView(compactItem(item.name, expiryLabel(item)));
            }
        }
        if (!anyExpiring) expiring.addView(small("Nothing expires in the next 7 days."));
        content.addView(collapsibleSection("expiring", "Expiring Soon", expiringCount() + " item(s) need attention", expiring));

        LinearLayout inventory = column();
        inventory.addView(locationSummary());
        content.addView(collapsibleSection("inventory", "Inventory Snapshot", "Counts by kitchen zone", inventory));

        LinearLayout plan = column();
        for (String idea : mealIdeas()) plan.addView(compactItem(idea, "Built from what is already in the pantry."));
        Button meals = secondary("Open meal ideas");
        meals.setOnClickListener(v -> { activeTab = 4; buildUi(); });
        plan.addView(meals);
        content.addView(collapsibleSection("meals", "Meal Direction", "A quick look without leaving the dashboard", plan));
    }

    private void renderScanner() {
        content.addView(sectionTitle("Item Scanner"));
        LinearLayout intro = column();
        intro.addView(title("Photo To Pantry Item"));
        intro.addView(small("Capture the front label, then angle the package so the expiry date and quantity are visible. Review every drafted field before adding it."));
        content.addView(panel(intro));

        LinearLayout capture = column();
        LinearLayout buttons = row();
        Button importImage = button("Import photo");
        Button takePhoto = secondary("Take photo");
        buttons.addView(importImage, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams cameraLp = new LinearLayout.LayoutParams(0, -2, 1);
        cameraLp.setMargins(dp(8), 0, 0, 0);
        buttons.addView(takePhoto, cameraLp);
        capture.addView(buttons);
        Button samplePhoto = secondary("Test sample label");
        capture.addView(samplePhoto, matchWrap());
        TextView captureGuide = small("Step 1: front label. Step 2: expiry date. Step 3: quantity or multipack count.");
        captureGuide.setPadding(0, dp(8), 0, 0);
        capture.addView(captureGuide);
        if (scannerBitmap != null) {
            ImageView preview = new ImageView(this);
            preview.setImageBitmap(scannerBitmap);
            preview.setAdjustViewBounds(true);
            preview.setMaxHeight(dp(220));
            preview.setBackground(rounded(0xfffff4f6, 0xffffccd5, 8));
            capture.addView(preview, matchWrap());
        }
        content.addView(panel(capture));
        importImage.setOnClickListener(v -> {
            if (!requirePlan("Photo import review", PantryRules.PLAN_PLUS)) return;
            launchImageImport();
        });
        takePhoto.setOnClickListener(v -> {
            if (!requirePlan("Camera review", PantryRules.PLAN_PLUS)) return;
            launchCameraCapture();
        });
        samplePhoto.setOnClickListener(v -> runSampleScannerParserTest());

        LinearLayout parser = column();
        parser.addView(title("Parse Label Text"));
        parser.addView(small("Paste text copied from your camera OCR, package label, receipt, or notes. PantryPilot drafts the item fields locally."));
        EditText labelText = multiTextField("Paste label text");
        labelText.setText(scanLabelTextDraft);
        parser.addView(labelText, matchWrap());
        Button parseText = secondary("Parse label text");
        parser.addView(parseText);
        content.addView(panel(parser));
        parseText.setOnClickListener(v -> {
            if (!requirePlan("Label text parsing", PantryRules.PLAN_PLUS)) return;
            parseLabelText(labelText);
        });

        LinearLayout review = column();
        EditText name = textField("Pantry item name");
        name.setText(scanNameDraft);
        EditText quantity = textField("Quantity, e.g. 2 cans");
        quantity.setText(scanQuantityDraft);
        EditText expiry = dateField("Expiry YYYY-MM-DD");
        expiry.setText(scanExpiryDraft);
        Spinner location = spinner("Pantry", "Fridge", "Freezer", "Counter", "Other");
        selectSpinner(location, scanLocationDraft);
        Spinner category = spinner("Protein", "Vegetable", "Fruit", "Grain", "Dairy", "Snack", "Sauce", "Other");
        selectSpinner(category, scanCategoryDraft);
        review.addView(twoViews("Name", name, "Qty", quantity));
        review.addView(twoViews("Location", location, "Category", category));
        review.addView(labeled("Expiry date", expiry));
        LinearLayout reviewButtons = row();
        Button draft = secondary("Draft item fields");
        Button add = button("Add pantry item");
        reviewButtons.addView(draft, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(0, -2, 1);
        addParams.setMargins(dp(8), 0, 0, 0);
        reviewButtons.addView(add, addParams);
        review.addView(reviewButtons);
        content.addView(panel(review));

        draft.setOnClickListener(v -> {
            if (!requirePlan("Scanner field drafting", PantryRules.PLAN_PLUS)) return;
            scanNameDraft = PantryRules.normalizeName(clean(name));
            scanQuantityDraft = PantryRules.normalizeQuantity(clean(quantity));
            scanExpiryDraft = clean(expiry);
            scanLocationDraft = location.getSelectedItem().toString();
            scanCategoryDraft = category.getSelectedItem().toString();
            draftScannerFields();
            buildUi();
        });
        add.setOnClickListener(v -> addScannedItem(name, quantity, expiry, location, category));
    }

    private void renderPantry() {
        content.addView(sectionTitle("Categorized Inventory"));
        Button sharePantry = secondary("Share pantry");
        sharePantry.setOnClickListener(v -> {
            if (!requirePlan("Pantry sharing", PantryRules.PLAN_PLUS)) return;
            shareText("Share pantry", "PantryPilot pantry export", pantryExportText());
        });
        content.addView(sharePantry, matchWrap());
        if (pantry.isEmpty()) {
            content.addView(empty("No pantry items yet."));
        } else {
            String[] locations = {"Fridge", "Freezer", "Pantry", "Counter", "Other"};
            for (String locationName : locations) {
                boolean has = false;
                for (PantryItem item : pantry) {
                    if (item.location.equals(locationName)) has = true;
                }
                if (!has) continue;
                content.addView(sectionTitle(locationName));
                content.addView(pantryGrid(locationName));
            }
        }

        LinearLayout form = column();
        EditText name = textField("Pantry item name");
        EditText quantity = textField("Quantity, e.g. 2 cans");
        EditText expiry = dateField("Expiry YYYY-MM-DD");
        Spinner location = spinner("Pantry", "Fridge", "Freezer", "Counter", "Other");
        Spinner category = spinner("Protein", "Vegetable", "Fruit", "Grain", "Dairy", "Snack", "Sauce", "Other");
        form.addView(twoViews("Name", name, "Qty", quantity));
        form.addView(twoViews("Location", location, "Category", category));
        form.addView(labeled("Expiry date", expiry));
        Button add = button("Add pantry item");
        form.addView(add);
        content.addView(collapsibleSection("manualAdd", "Add Pantry Item", "Manual entry for loose pantry items", form));

        add.setOnClickListener(v -> {
            String itemName = PantryRules.normalizeName(clean(name));
            if (itemName.isEmpty()) {
                setStatus("Enter an item name.");
                return;
            }
            if (!canAddPantryOrShowUpgrade()) return;
            String exp = PantryRules.normalizeDateInput(clean(expiry));
            if (!PantryRules.validDate(exp)) {
                setStatus("Use expiry format YYYY-MM-DD or YYYYMMDD.");
                return;
            }
            pantry.add(new PantryItem(itemName,
                    location.getSelectedItem().toString(),
                    PantryRules.normalizeQuantity(clean(quantity)),
                    category.getSelectedItem().toString(),
                    exp));
            saveData();
            setStatus("Added " + itemName + ".");
            buildUi();
        });
    }

    private void renderGrocery() {
        content.addView(sectionTitle("Grocery List"));
        LinearLayout form = row();
        EditText item = textField("Add grocery item");
        Button add = button("Add");
        form.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        form.addView(add);
        content.addView(panel(form));
        add.setOnClickListener(v -> {
            String name = PantryRules.normalizeName(clean(item));
            if (name.isEmpty()) {
                setStatus("Enter a grocery item.");
                return;
            }
            if (PantryRules.containsShopName(shoppingNames(), name)) {
                setStatus(name + " is already on the grocery list.");
                return;
            }
            if (!canAddGroceryOrShowUpgrade()) return;
            shopping.add(new ShopItem(name, false));
            saveData();
            setStatus("Added " + name + " to grocery list.");
            buildUi();
        });

        Button shareGrocery = secondary("Share grocery list");
        shareGrocery.setOnClickListener(v -> {
            if (!requirePlan("Grocery sharing", PantryRules.PLAN_PLUS)) return;
            shareText("Share grocery list", "PantryPilot grocery list", groceryExportText());
        });
        content.addView(shareGrocery, matchWrap());

        if (shopping.isEmpty()) {
            content.addView(empty("Grocery list is empty."));
            return;
        }
        String[] groups = {"Produce", "Protein", "Dairy", "Pantry", "Household", "Other"};
        for (String group : groups) {
            boolean has = false;
            for (ShopItem s : shopping) if (groceryGroup(s.name).equals(group)) has = true;
            if (!has) continue;
            content.addView(sectionTitle(group));
            for (int i = 0; i < shopping.size(); i++) {
                ShopItem s = shopping.get(i);
                if (!groceryGroup(s.name).equals(group)) continue;
                int index = i;
                LinearLayout card = row();
                CheckBox check = new CheckBox(this);
                check.setChecked(s.done);
                TextView label = small(s.name);
                label.setTextSize(17);
                Button remove = secondary("Remove");
                card.addView(check);
                card.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
                card.addView(remove);
                check.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    s.done = isChecked;
                    saveData();
                    updateSummary();
                });
                remove.setOnClickListener(v -> {
                    shopping.remove(index);
                    saveData();
                    buildUi();
                });
                content.addView(panel(card));
            }
        }
        Button clearDone = secondary("Clear checked items");
        clearDone.setOnClickListener(v -> {
            int cleared = 0;
            for (int i = shopping.size() - 1; i >= 0; i--) {
                if (shopping.get(i).done) {
                    shopping.remove(i);
                    cleared++;
                }
            }
            setStatus(cleared == 0 ? "No checked grocery items to clear."
                    : "Cleared " + cleared + " checked grocery item" + (cleared == 1 ? "." : "s."));
            saveData();
            buildUi();
        });
        content.addView(clearDone, matchWrap());
    }

    private void renderMeals() {
        content.addView(sectionTitle("Cook From Pantry Items"));
        LinearLayout explainer = column();
        explainer.addView(small("Meal ideas are generated locally from pantry categories so the feature works offline without accounts or internet access."));
        Button refresh = button("Refresh meal ideas");
        explainer.addView(refresh);
        content.addView(panel(explainer));
        refresh.setOnClickListener(v -> renderActiveTab());

        List<String> ideas = mealIdeas();
        int limit = Math.min(PantryRules.mealIdeaLimit(currentPlan), ideas.size());
        for (int i = 0; i < limit; i++) {
            String idea = ideas.get(i);
            LinearLayout card = column();
            TextView t = title(idea);
            t.setTextSize(17);
            card.addView(t);
            card.addView(small("Use pantry items nearing their expiry date first. Add missing ingredients to the grocery list."));
            Button addMissing = secondary("Add common extras");
            addMissing.setOnClickListener(v -> {
                if (!requirePlan("Meal extras", PantryRules.PLAN_PRO)) return;
                if (!canAddGroceryOrShowUpgrade()) return;
                addIfMissing("Onions");
                addIfMissing("Garlic");
                addIfMissing("Rice");
                saveData();
                setStatus("Common extras added to grocery list.");
            });
            card.addView(addMissing);
            content.addView(panel(card));
        }
        if (limit < ideas.size()) {
            LinearLayout locked = column();
            locked.addView(title("More meal ideas"));
            locked.addView(small("Plus unlocks the full local meal idea set. Pro adds one-tap common extras."));
            Button openPlans = secondary("View plans");
            openPlans.setOnClickListener(v -> {
                activeTab = 5;
                buildUi();
            });
            locked.addView(openPlans);
            content.addView(panel(locked));
        }
    }

    private View heroCard() {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView eyebrow = small("Kitchen Command Center");
        eyebrow.setTextColor(0xffffe5ec);
        TextView head = title("Know what to use next");
        head.setTextColor(Color.WHITE);
        TextView body = small("Scan labels, track expiry dates, build grocery lists, and cook from what is already paid for.");
        body.setTextColor(0xfffff0f4);
        card.addView(eyebrow);
        card.addView(head);
        card.addView(body);
        card.setBackground(gradient(0xffd94f70, 0xff3867d6, 8));
        card.setElevation(dp(8));
        card.setTranslationZ(dp(2));
        return card;
    }

    private View planStatusCard() {
        LinearLayout card = column();
        TextView name = title(currentPlan + " plan");
        name.setTextSize(18);
        card.addView(name);
        card.addView(small(PantryRules.planSummary(currentPlan)));
        card.addView(small(adsRemoved()
                ? "Ads removed on this device."
                : "Free plan shows a small non-intrusive banner."));
        card.addView(small("Usage: " + pantry.size() + "/" + PantryRules.pantryLimit(currentPlan) +
                " pantry, " + shopping.size() + "/" + PantryRules.groceryLimit(currentPlan) + " grocery."));
        Button manage = secondary("Manage plan");
        manage.setOnClickListener(v -> {
            activeTab = 5;
            buildUi();
        });
        card.addView(manage);
        return panel(card);
    }

    private View freeAdBanner() {
        if (adsRemoved()) return null;
        if (!PantryRules.PLAN_FREE.equals(currentPlan)) return null;
        LinearLayout banner = column();
        banner.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = small("Sponsored");
        label.setTextColor(TEXT);
        LinearLayout text = column();
        text.addView(label);
        currentAdStatus = small(mobileAdsReady ? "Loading ad..." : "Preparing privacy choices...");
        text.addView(currentAdStatus);
        heading.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        Button remove = secondary("Remove ads");
        remove.setOnClickListener(v -> {
            activeTab = 5;
            buildUi();
        });
        heading.addView(remove);
        banner.addView(heading);

        currentAdHost = new FrameLayout(this);
        currentAdHost.setMinimumHeight(dp(50));
        currentAdHost.setPadding(0, dp(6), 0, 0);
        banner.addView(currentAdHost, new LinearLayout.LayoutParams(-1, -2));

        currentPrivacyOptions = secondary("Ad privacy");
        currentPrivacyOptions.setVisibility(isPrivacyOptionsRequired() ? View.VISIBLE : View.GONE);
        currentPrivacyOptions.setOnClickListener(v -> showAdPrivacyOptions());
        banner.addView(currentPrivacyOptions, matchWrap());

        banner.setBackground(rounded(0xfffffbeb, 0xffffd166, 8));
        banner.setElevation(dp(3));
        if (mobileAdsReady) loadBannerAd();
        return banner;
    }

    private boolean isAdMobConfigured() {
        return looksLikeAdMobBannerId(BuildConfig.ADMOB_BANNER_ID);
    }

    private void requestAdConsent() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        formError -> {
                            refreshAdPrivacyUi();
                            if (consentInformation.canRequestAds()) initializeMobileAds();
                            else updateAdStatus(formError == null
                                    ? "Ads unavailable until privacy choices are complete."
                                    : "Ad privacy form unavailable.");
                        }),
                requestError -> {
                    refreshAdPrivacyUi();
                    if (consentInformation.canRequestAds()) initializeMobileAds();
                    else updateAdStatus("Ads unavailable while privacy choices cannot be checked.");
                });
        if (consentInformation.canRequestAds()) initializeMobileAds();
    }

    private void initializeMobileAds() {
        if (mobileAdsReady || mobileAdsInitializing || !isAdMobConfigured()) return;
        mobileAdsInitializing = true;
        MobileAds.initialize(this, initializationStatus -> runOnUiThread(() -> {
            mobileAdsInitializing = false;
            mobileAdsReady = true;
            updateAdStatus("Loading ad...");
            loadBannerAd();
        }));
    }

    private void loadBannerAd() {
        if (!mobileAdsReady || currentAdHost == null || currentAdView != null
                || adsRemoved() || !PantryRules.PLAN_FREE.equals(currentPlan)) return;
        if (consentInformation != null && !consentInformation.canRequestAds()) return;

        int widthPixels = getResources().getDisplayMetrics().widthPixels - dp(52);
        int widthDp = Math.max(200, Math.round(widthPixels / getResources().getDisplayMetrics().density));
        AdView adView = new AdView(this);
        adView.setAdUnitId(BuildConfig.ADMOB_BANNER_ID);
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, widthDp));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                updateAdStatus(BuildConfig.DEBUG ? "Test ad loaded." : "Advertisement");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                updateAdStatus(BuildConfig.DEBUG
                        ? "Test ad did not load. Check connection and AdMob logs."
                        : "Advertisement temporarily unavailable.");
            }
        });
        currentAdView = adView;
        currentAdHost.removeAllViews();
        currentAdHost.addView(adView, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        adView.loadAd(new AdRequest.Builder().build());
    }

    private boolean isPrivacyOptionsRequired() {
        return consentInformation != null
                && consentInformation.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    private void refreshAdPrivacyUi() {
        runOnUiThread(() -> {
            if (currentPrivacyOptions != null) {
                currentPrivacyOptions.setVisibility(isPrivacyOptionsRequired() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showAdPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(this, formError -> {
            refreshAdPrivacyUi();
            if (formError != null) updateAdStatus("Ad privacy options are temporarily unavailable.");
            if (consentInformation != null && consentInformation.canRequestAds()) initializeMobileAds();
        });
    }

    private void updateAdStatus(String message) {
        runOnUiThread(() -> {
            if (currentAdStatus != null) currentAdStatus.setText(message);
        });
    }

    private void destroyCurrentAdView() {
        if (currentAdView != null) {
            currentAdView.destroy();
            currentAdView = null;
        }
    }

    private boolean isProductionBillingConfigured() {
        return BILLING_MODE_PRODUCTION.equals(BILLING_MODE)
                && !PantryRules.REMOVE_ADS_PRODUCT_ID.trim().isEmpty()
                && !PantryRules.PLUS_PRODUCT_ID.trim().isEmpty()
                && !PantryRules.PRO_PRODUCT_ID.trim().isEmpty();
    }

    private boolean adsRemoved() {
        return PantryRules.planRemovesAds(currentPlan) || prefs.getBoolean(PREF_AD_FREE, false);
    }

    private boolean looksLikeAdMobBannerId(String value) {
        return value != null && value.matches("ca-app-pub-\\d{16}/\\d{10}");
    }

    private void renderPlans() {
        content.addView(sectionTitle("Plans And Demo Purchase"));
        LinearLayout intro = column();
        intro.addView(title(currentPlan + " plan active"));
        intro.addView(small(isProductionBillingConfigured()
                ? "Production billing mode is selected. Wire this screen to Google Play Billing before accepting payments."
                : "Demo purchase changes a local entitlement for testing Play Console copy and gated UI. It does not charge money and does not contact Google Play yet."));
        intro.addView(small("Default IDs keep monetization inactive. Replace IDs and add the SDK boot code when Play Console and AdMob are ready."));
        content.addView(panel(intro));

        content.addView(removeAdsCard());

        content.addView(planCard(PantryRules.PLAN_FREE,
                "For trying the core pantry tracker.",
                "12 pantry items, 12 grocery items, sample scanner preview, 2 meal ideas.",
                ""));
        content.addView(planCard(PantryRules.PLAN_PLUS,
                "For regular household use.",
                "150 pantry items, 100 grocery items, photo review, label text parsing, sharing, and full meal ideas.",
                PantryRules.PLUS_PRODUCT_ID));
        content.addView(planCard(PantryRules.PLAN_PRO,
                "For power users and large kitchens.",
                "500 pantry items, 300 grocery items, scanner, sharing, full meal ideas, and meal extras.",
                PantryRules.PRO_PRODUCT_ID));

        LinearLayout restore = column();
        restore.addView(title("Demo Restore"));
        restore.addView(small("Restores the highest local demo purchase saved on this device. Real restore will be handled by Google Play Billing later."));
        Button restoreButton = secondary("Restore demo purchase");
        restoreButton.setOnClickListener(v -> restoreDemoPurchase());
        restore.addView(restoreButton);
        content.addView(panel(restore));
    }

    private View removeAdsCard() {
        LinearLayout card = column();
        TextView heading = title("Remove Ads");
        heading.setTextSize(20);
        card.addView(heading);
        card.addView(small("$0.99 one-time unlock for users who like Free limits but want the sponsor space gone."));
        card.addView(small("Play product ID: " + PantryRules.REMOVE_ADS_PRODUCT_ID));
        Button action;
        if (adsRemoved()) {
            action = secondary("Ads removed");
            action.setEnabled(false);
        } else {
            action = button("Demo purchase Remove Ads");
            action.setOnClickListener(v -> confirmDemoRemoveAds());
        }
        card.addView(action);
        if (prefs.getBoolean(PREF_AD_FREE, false)) {
            Button reset = secondary("Reset ad-free demo");
            reset.setOnClickListener(v -> {
                setAdsRemoved(false, false);
                setStatus("Demo ad-free entitlement reset.");
                buildUi();
            });
            card.addView(reset);
        }
        return panel(card);
    }

    private View planCard(String plan, String pitch, String features, String productId) {
        LinearLayout card = column();
        TextView heading = title(plan);
        heading.setTextSize(20);
        card.addView(heading);
        card.addView(small(pitch));
        card.addView(small(features));
        if (!productId.isEmpty()) {
            card.addView(small("Play product ID: " + productId));
        }
        Button action;
        if (plan.equals(currentPlan)) {
            action = secondary("Current plan");
            action.setEnabled(false);
        } else if (PantryRules.PLAN_FREE.equals(plan)) {
            action = secondary("Reset demo to Free");
            action.setOnClickListener(v -> {
                setPlan(PantryRules.PLAN_FREE, false);
                setStatus("Demo entitlement reset to Free.");
                buildUi();
            });
        } else {
            action = button("Demo purchase " + plan);
            action.setOnClickListener(v -> confirmDemoPurchase(plan));
        }
        card.addView(action);
        return panel(card);
    }

    private void confirmDemoPurchase(String plan) {
        String normalized = PantryRules.normalizePlan(plan);
        new AlertDialog.Builder(this)
                .setTitle("Demo purchase " + normalized)
                .setMessage("This unlocks " + normalized + " locally for testing only. It does not charge money. Replace this with Google Play Billing before accepting real payments.")
                .setPositiveButton("Unlock demo", (dialog, which) -> {
                    setPlan(normalized, true);
                    setStatus("Demo " + normalized + " purchase unlocked.");
                    buildUi();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDemoRemoveAds() {
        new AlertDialog.Builder(this)
                .setTitle("Demo purchase Remove Ads")
                .setMessage("This removes the Free plan banner locally for testing only. It does not charge money. Replace this with Google Play Billing before accepting real payments.")
                .setPositiveButton("Unlock demo", (dialog, which) -> {
                    setAdsRemoved(true, true);
                    setStatus("Demo Remove Ads purchase unlocked.");
                    buildUi();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreDemoPurchase() {
        String purchased = PantryRules.normalizePlan(prefs.getString(PREF_DEMO_PURCHASED_PLAN, PantryRules.PLAN_FREE));
        setPlan(purchased, false);
        setAdsRemoved(prefs.getBoolean(PREF_DEMO_PURCHASED_AD_FREE, false), false);
        setStatus("Restored demo entitlement: " + purchased + (adsRemoved() ? " with ads removed." : "."));
        buildUi();
    }

    private void setPlan(String plan, boolean recordPurchase) {
        currentPlan = PantryRules.normalizePlan(plan);
        SharedPreferences.Editor editor = prefs.edit().putString(PREF_PLAN, currentPlan);
        if (recordPurchase && PantryRules.planRank(currentPlan) > PantryRules.planRank(prefs.getString(PREF_DEMO_PURCHASED_PLAN, PantryRules.PLAN_FREE))) {
            editor.putString(PREF_DEMO_PURCHASED_PLAN, currentPlan);
        }
        editor.apply();
    }

    private void setAdsRemoved(boolean removed, boolean recordPurchase) {
        SharedPreferences.Editor editor = prefs.edit().putBoolean(PREF_AD_FREE, removed);
        if (recordPurchase && removed) {
            editor.putBoolean(PREF_DEMO_PURCHASED_AD_FREE, true);
        } else if (!removed) {
            editor.putBoolean(PREF_DEMO_PURCHASED_AD_FREE, false);
        }
        editor.apply();
    }

    private void showTutorialStep(int step) {
        String[] titles = {
                "Welcome to PantryPilot",
                "Add food once",
                "Use what expires next",
                "Upgrade when needed"
        };
        String[] bodies = {
                "PantryPilot keeps pantry, grocery, expiry, and meal planning local on this device.",
                "Start with Scan for photo-assisted entry or Pantry for quick manual entry. Plus unlocks label text parsing and sharing.",
                "The dashboard highlights expiring food so you can cook what is already paid for before buying more.",
                "Free includes starter limits and a small sponsor space. Remove Ads hides banners only. Plus and Pro remove ads and raise limits through the Plans screen."
        };
        int last = titles.length - 1;
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(titles[step])
                .setMessage(bodies[step])
                .setNegativeButton("Skip", (dialog, which) -> finishTutorial())
                .setCancelable(false);
        if (step < last) {
            builder.setPositiveButton("Next", (dialog, which) -> showTutorialStep(step + 1));
        } else {
            builder.setPositiveButton("Start", (dialog, which) -> finishTutorial());
        }
        builder.show();
    }

    private void finishTutorial() {
        prefs.edit().putBoolean(PREF_TUTORIAL_SEEN, true).apply();
        setStatus("Tutorial complete. Add your first pantry item or review the sample data.");
    }

    private boolean requirePlan(String feature, String requiredPlan) {
        if (PantryRules.isAtLeast(currentPlan, requiredPlan)) return true;
        setStatus(feature + " requires " + requiredPlan + ". Open Plans for the demo unlock.");
        activeTab = 5;
        buildUi();
        return false;
    }

    private boolean canAddPantryOrShowUpgrade() {
        if (PantryRules.canAddPantryItem(currentPlan, pantry.size())) return true;
        setStatus("Pantry limit reached for " + currentPlan + ". Open Plans to raise the limit.");
        activeTab = 5;
        buildUi();
        return false;
    }

    private boolean canAddGroceryOrShowUpgrade() {
        if (PantryRules.canAddGroceryItem(currentPlan, shopping.size())) return true;
        setStatus("Grocery limit reached for " + currentPlan + ". Open Plans to raise the limit.");
        activeTab = 5;
        buildUi();
        return false;
    }

    private View statCard(String label, String value, String note) {
        LinearLayout card = column();
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        TextView v = title(value);
        v.setTextSize(24);
        TextView l = small(label);
        l.setTextColor(TEXT);
        l.setTypeface(null, Typeface.BOLD);
        card.addView(v);
        card.addView(l);
        card.addView(small(note));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(5), dp(8), dp(10));
        card.setLayoutParams(lp);
        card.setBackground(rounded(statColor(label), statStroke(label), 8));
        card.setElevation(dp(5));
        card.setTranslationZ(dp(1));
        return card;
    }

    private int statColor(String label) {
        if (label.contains("Pantry")) return 0xfffff1f3;
        if (label.contains("Expiring")) return 0xfffff8df;
        if (label.contains("Grocery")) return 0xffeef7fb;
        return 0xfff1f5ff;
    }

    private int statStroke(String label) {
        if (label.contains("Pantry")) return 0xffffccd5;
        if (label.contains("Expiring")) return 0xffffe08a;
        if (label.contains("Grocery")) return 0xffb9e4ee;
        return 0xffc7d2fe;
    }

    private View collapsibleSection(String key, String title, String subtitle, View body) {
        LinearLayout wrap = column();
        wrap.setPadding(0, 0, 0, 0);
        wrap.setBackground(rounded(PANEL, LINE, 8));
        wrap.setElevation(dp(9));
        wrap.setTranslationZ(dp(3));
        wrap.addView(accentStrip(sectionAccent(key)), new LinearLayout.LayoutParams(-1, dp(5)));

        TextView header = title((expandedHome.contains(key) ? "v  " : ">  ") + title);
        header.setTextSize(18);
        TextView sub = small(subtitle);
        LinearLayout inner = column();
        inner.setPadding(dp(14), dp(13), dp(14), dp(14));
        inner.addView(header);
        inner.addView(sub);
        if (expandedHome.contains(key)) {
            LinearLayout.LayoutParams bodyParams = matchWrap();
            bodyParams.setMargins(0, dp(10), 0, 0);
            inner.addView(body, bodyParams);
        }
        wrap.addView(inner);
        View.OnClickListener toggle = v -> {
            pendingScrollY = currentScroll == null ? 0 : currentScroll.getScrollY();
            if (expandedHome.contains(key)) {
                expandedHome.remove(key);
            } else {
                expandedHome.add(key);
            }
            buildUi();
        };
        header.setOnClickListener(toggle);
        sub.setOnClickListener(toggle);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(8), 0, dp(16));
        wrap.setLayoutParams(lp);
        return wrap;
    }

    private int sectionAccent(String key) {
        if ("next".equals(key)) return GREEN;
        if ("expiring".equals(key)) return 0xffffb020;
        if ("inventory".equals(key)) return 0xff18a0a9;
        if ("meals".equals(key)) return GREEN_DARK;
        if ("manualAdd".equals(key)) return GREEN;
        return 0xff7c3aed;
    }

    private View compactItem(String primary, String secondary) {
        LinearLayout layout = column();
        layout.setPadding(0, dp(7), 0, dp(7));
        TextView p = small(primary);
        p.setTextSize(16);
        p.setTextColor(TEXT);
        p.setTypeface(null, Typeface.BOLD);
        layout.addView(p);
        layout.addView(small(secondary));
        return layout;
    }

    private GradientDrawable gradient(int start, int end, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{start, end});
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void launchImageImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQ_SCAN_IMAGE);
        } catch (ActivityNotFoundException e) {
            setStatus("No image picker is available on this device.");
        }
    }

    private void launchCameraCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            pendingCameraUri = createCameraOutputUri();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_SCAN_CAMERA);
        } catch (ActivityNotFoundException e) {
            setStatus("No camera app is available on this device.");
        } catch (Exception e) {
            setStatus("Could not prepare camera capture: " + e.getMessage());
        }
    }

    private Uri createCameraOutputUri() {
        File dir = new File(getCacheDir(), "pantry_scans");
        if (!dir.exists()) dir.mkdirs();
        String name = "scan_" + System.currentTimeMillis() + ".jpg";
        return Uri.parse("content://" + getPackageName() + ".images/" + name);
    }

    private void runSampleScannerParserTest() {
        scannerBitmap = createSamplePackageBitmap();
        PantryRules.ScanDraft draft = PantryRules.parsePackagingText("Greek Yogurt\nNet Wt 500g\nBest Before 2026-12-31\nKeep refrigerated");
        scanNameDraft = draft.name;
        scanQuantityDraft = draft.quantity;
        scanExpiryDraft = draft.expiry;
        scanLocationDraft = draft.location;
        scanCategoryDraft = draft.category;
        setStatus("Sample label drafted item fields. Review the details before adding.");
        activeTab = 1;
        pendingScrollY = dp(520);
        buildUi();
    }

    private void processScannerBitmap(Bitmap bitmap, String source) {
        if (bitmap == null) {
            setStatus("No image available for scanning.");
            return;
        }
        setStatus("Image loaded from " + source + ". Review the item fields before adding.");
        activeTab = 1;
        pendingScrollY = dp(520);
        buildUi();
    }

    private void parseLabelText(EditText labelText) {
        scanLabelTextDraft = labelText.getText().toString().trim();
        if (scanLabelTextDraft.isEmpty()) {
            setStatus("Paste label text before parsing.");
            return;
        }
        PantryRules.ScanDraft draft = PantryRules.parsePackagingText(scanLabelTextDraft);
        scanNameDraft = draft.name;
        scanQuantityDraft = draft.quantity;
        scanExpiryDraft = draft.expiry;
        scanLocationDraft = draft.location;
        scanCategoryDraft = draft.category;
        if (scanNameDraft.isEmpty()) {
            setStatus("No product name found. Add the name manually, then review the draft.");
        } else {
            setStatus("Label text parsed. Review name, expiry, quantity, and location before adding.");
        }
        activeTab = 1;
        pendingScrollY = dp(760);
        buildUi();
    }

    private Bitmap createSamplePackageBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(0xfffff8ec);

        paint.setColor(0xffd94f70);
        canvas.drawRect(0, 0, 1200, 160, paint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(68);
        canvas.drawText("PantryPilot Test Pack", 70, 100, paint);

        paint.setColor(0xff18212f);
        paint.setTextSize(82);
        canvas.drawText("Greek Yogurt", 80, 270, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(48);
        canvas.drawText("Net Wt 500g", 82, 370, paint);
        canvas.drawText("Best Before 2026-12-31", 82, 470, paint);
        canvas.drawText("Keep refrigerated", 82, 570, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setColor(0xff3867d6);
        canvas.drawRoundRect(40, 190, 1160, 630, 28, 28, paint);
        paint.setStyle(Paint.Style.FILL);
        return bitmap;
    }

    private void draftScannerFields() {
        if (scanNameDraft.isEmpty()) {
            for (ShopItem item : shopping) {
                if (!item.done) {
                    scanNameDraft = item.name;
                    break;
                }
            }
        }
        if (scanNameDraft.isEmpty()) scanNameDraft = "New pantry item";
        scanQuantityDraft = PantryRules.normalizeQuantity(scanQuantityDraft);
        if (scanExpiryDraft.isEmpty()) scanExpiryDraft = LocalDate.now().plusDays(14).toString();
        scanCategoryDraft = guessCategory(scanNameDraft);
        scanLocationDraft = PantryRules.guessLocation(scanNameDraft, scanCategoryDraft);
        setStatus("Draft created. Confirm name, expiry date, quantity, and location before adding.");
    }

    private void addScannedItem(EditText name, EditText quantity, EditText expiry, Spinner location, Spinner category) {
        String itemName = PantryRules.normalizeName(clean(name));
        if (itemName.isEmpty()) {
            setStatus("Confirm the item name before adding.");
            return;
        }
        if (!canAddPantryOrShowUpgrade()) return;
        String exp = PantryRules.normalizeDateInput(clean(expiry));
        if (!PantryRules.validDate(exp)) {
            setStatus("Use expiry format YYYY-MM-DD or YYYYMMDD.");
            return;
        }
        pantry.add(new PantryItem(itemName,
                location.getSelectedItem().toString(),
                PantryRules.normalizeQuantity(clean(quantity)),
                category.getSelectedItem().toString(),
                exp));
        saveData();
        scanNameDraft = "";
        scanQuantityDraft = "1";
        scanExpiryDraft = "";
        scanLocationDraft = "Pantry";
        scanCategoryDraft = "Other";
        scannerBitmap = null;
        scanLabelTextDraft = "";
        setStatus("Pantry item added: " + itemName + ".");
        activeTab = 2;
        buildUi();
    }

    private String guessCategory(String name) {
        return PantryRules.guessCategory(name);
    }

    private void selectSpinner(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private View locationSummary() {
        LinearLayout wrap = column();
        String[] locations = {"Fridge", "Freezer", "Pantry", "Counter", "Other"};
        for (String location : locations) {
            int count = 0;
            for (PantryItem item : pantry) if (item.location.equals(location)) count++;
            if (count > 0) wrap.addView(small(location + ": " + count + " items"));
        }
        if (pantry.isEmpty()) wrap.addView(small("No inventory yet."));
        return wrap;
    }

    private View itemCard(PantryItem item, int index, boolean actionsEnabled) {
        LinearLayout card = column();
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(rounded(0xffffffff, categoryStroke(item.category), 8));
        card.setElevation(dp(4));
        card.setTranslationZ(dp(1));

        LinearLayout top = row();
        TextView icon = categoryBadge(item.category);
        LinearLayout text = column();
        TextView nameLine = title(item.name);
        nameLine.setTextSize(15);
        TextView meta = small(item.quantity + " - " + item.category);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(nameLine);
        text.addView(meta);
        top.addView(icon);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(8), 0, 0, 0);
        top.addView(text, textParams);
        card.addView(top);
        TextView expiry = small(expiryLabel(item));
        expiry.setPadding(0, dp(8), 0, 0);
        card.addView(expiry);
        if (actionsEnabled) {
            LinearLayout firstRow = row();
            LinearLayout secondRow = row();
            Button addToList = secondary("Add to list");
            Button used = secondary("Mark used");
            Button delete = secondary("Delete");
            firstRow.addView(addToList, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams usedParams = new LinearLayout.LayoutParams(0, -2, 1);
            usedParams.setMargins(dp(6), 0, 0, 0);
            firstRow.addView(used, usedParams);
            secondRow.addView(delete, new LinearLayout.LayoutParams(-1, -2));
            LinearLayout.LayoutParams firstParams = matchWrap();
            firstParams.setMargins(0, dp(8), 0, dp(6));
            card.addView(firstRow, firstParams);
            card.addView(secondRow);
            addToList.setOnClickListener(v -> {
                if (!canAddGroceryOrShowUpgrade()) return;
                if (PantryRules.containsShopName(shoppingNames(), item.name)) {
                    setStatus(item.name + " is already on the grocery list.");
                    return;
                }
                shopping.add(new ShopItem(item.name, false));
                saveData();
                setStatus(item.name + " added to grocery list.");
            });
            used.setOnClickListener(v -> {
                pantry.remove(index);
                saveData();
                setStatus("Marked used: " + item.name + ".");
                buildUi();
            });
            delete.setOnClickListener(v -> {
                pantry.remove(index);
                saveData();
                setStatus("Deleted " + item.name + ".");
                buildUi();
            });
        }
        return card;
    }

    private View pantryGrid(String locationName) {
        LinearLayout grid = column();
        LinearLayout currentRow = null;
        int column = 0;
        for (int i = 0; i < pantry.size(); i++) {
            PantryItem item = pantry.get(i);
            if (!item.location.equals(locationName)) continue;
            if (column == 0) {
                currentRow = row();
                grid.addView(currentRow, matchWrap());
            }
            View card = itemCard(item, i, true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
            if (column == 0) {
                params.setMargins(0, 0, dp(6), 0);
            } else {
                params.setMargins(dp(6), 0, 0, 0);
            }
            currentRow.addView(card, params);
            column++;
            if (column == 2) column = 0;
        }
        if (currentRow != null && column == 1) {
            View spacer = new View(this);
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, -2, 1);
            spacerParams.setMargins(dp(6), 0, 0, 0);
            currentRow.addView(spacer, spacerParams);
        }
        LinearLayout wrap = column();
        wrap.addView(grid);
        return panel(wrap);
    }

    private TextView categoryBadge(String category) {
        TextView badge = small(categoryCode(category));
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(Color.WHITE);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextSize(14);
        badge.setMinWidth(dp(40));
        badge.setMinHeight(dp(40));
        badge.setBackground(rounded(categoryColor(category), categoryColor(category), 20));
        return badge;
    }

    private String categoryCode(String category) {
        if ("Protein".equals(category)) return "PR";
        if ("Vegetable".equals(category)) return "VG";
        if ("Fruit".equals(category)) return "FR";
        if ("Grain".equals(category)) return "GR";
        if ("Dairy".equals(category)) return "DY";
        if ("Snack".equals(category)) return "SN";
        if ("Sauce".equals(category)) return "SC";
        return "IT";
    }

    private int categoryColor(String category) {
        if ("Protein".equals(category)) return 0xffd94f70;
        if ("Vegetable".equals(category)) return 0xff18a0a9;
        if ("Fruit".equals(category)) return 0xffff8f3d;
        if ("Grain".equals(category)) return 0xff8b5cf6;
        if ("Dairy".equals(category)) return 0xff3867d6;
        if ("Snack".equals(category)) return 0xfff59e0b;
        if ("Sauce".equals(category)) return 0xffef4444;
        return 0xff687386;
    }

    private int categoryStroke(String category) {
        if ("Protein".equals(category)) return 0xffffccd5;
        if ("Vegetable".equals(category)) return 0xffb9e4ee;
        if ("Fruit".equals(category)) return 0xffffd3a6;
        if ("Grain".equals(category)) return 0xffddd6fe;
        if ("Dairy".equals(category)) return 0xffc7d2fe;
        if ("Snack".equals(category)) return 0xffffe08a;
        if ("Sauce".equals(category)) return 0xffffc6c6;
        return LINE;
    }

    private int expiringCount() {
        int expiring = 0;
        for (PantryItem item : pantry) {
            Long days = daysUntil(item.expiry);
            if (days != null && days <= 7) expiring++;
        }
        return expiring;
    }

    private int openShoppingCount() {
        int open = 0;
        for (ShopItem item : shopping) if (!item.done) open++;
        return open;
    }

    private int usedLocationCount() {
        Set<String> locations = new HashSet<>();
        for (PantryItem item : pantry) {
            if (!item.location.trim().isEmpty()) locations.add(item.location);
        }
        return locations.size();
    }

    private List<String> shoppingNames() {
        List<String> names = new ArrayList<>();
        for (ShopItem item : shopping) names.add(item.name);
        return names;
    }

    private String groceryGroup(String name) {
        return PantryRules.groceryGroup(name);
    }

    private void updateSummary() {
        int expiring = 0;
        for (PantryItem item : pantry) {
            Long days = daysUntil(item.expiry);
            if (days != null && days <= 7) expiring++;
        }
        int openShop = 0;
        for (ShopItem item : shopping) if (!item.done) openShop++;
        summary.setText("Plan: " + currentPlan + "   Pantry: " + pantry.size() + "/" + PantryRules.pantryLimit(currentPlan) +
                "   Expiring soon: " + expiring + "   Grocery: " + openShop + "/" + PantryRules.groceryLimit(currentPlan) +
                "   Zones: " + usedLocationCount());
    }

    private List<String> mealIdeas() {
        List<String> names = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        for (PantryItem item : pantry) {
            names.add(item.name);
            categories.add(item.category);
        }
        return PantryRules.mealIdeas(names, categories);
    }

    private void shareText(String chooserTitle, String subject, String body) {
        if (body == null || body.trim().isEmpty()) {
            setStatus("Nothing to share yet.");
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(Intent.createChooser(send, chooserTitle));
            setStatus("Share sheet opened.");
        } catch (ActivityNotFoundException e) {
            setStatus("No app is available for sharing.");
        }
    }

    private String pantryExportText() {
        StringBuilder builder = new StringBuilder();
        builder.append("PantryPilot pantry export\n\n");
        if (pantry.isEmpty()) {
            builder.append("No pantry items yet.\n");
            return builder.toString();
        }
        String[] locations = {"Fridge", "Freezer", "Pantry", "Counter", "Other"};
        for (String location : locations) {
            boolean wroteHeader = false;
            for (PantryItem item : pantry) {
                if (!item.location.equals(location)) continue;
                if (!wroteHeader) {
                    builder.append(location).append('\n');
                    wroteHeader = true;
                }
                builder.append("- ")
                        .append(item.name)
                        .append(" | ")
                        .append(item.quantity)
                        .append(" | ")
                        .append(item.category)
                        .append(" | ")
                        .append(item.expiry.isEmpty() ? "No expiry date" : item.expiry)
                        .append('\n');
            }
            if (wroteHeader) builder.append('\n');
        }
        return builder.toString().trim();
    }

    private String groceryExportText() {
        StringBuilder builder = new StringBuilder();
        builder.append("PantryPilot grocery list\n\n");
        if (shopping.isEmpty()) {
            builder.append("Grocery list is empty.\n");
            return builder.toString();
        }
        String[] groups = {"Produce", "Protein", "Dairy", "Pantry", "Household", "Other"};
        for (String group : groups) {
            boolean wroteHeader = false;
            for (ShopItem item : shopping) {
                if (!groceryGroup(item.name).equals(group)) continue;
                if (!wroteHeader) {
                    builder.append(group).append('\n');
                    wroteHeader = true;
                }
                builder.append(item.done ? "[x] " : "[ ] ")
                        .append(item.name)
                        .append('\n');
            }
            if (wroteHeader) builder.append('\n');
        }
        return builder.toString().trim();
    }

    private String first(Map<String, List<String>> map, String key, String fallback) {
        List<String> list = map.get(key);
        return list == null || list.isEmpty() ? fallback : list.get(0);
    }

    private void addIfMissing(String name) {
        String cleanName = PantryRules.normalizeName(name);
        if (!PantryRules.containsShopName(shoppingNames(), cleanName) && PantryRules.canAddGroceryItem(currentPlan, shopping.size())) {
            shopping.add(new ShopItem(cleanName, false));
        }
    }

    private String expiryLabel(PantryItem item) {
        if (item.expiry.isEmpty()) return "No expiry date";
        Long days = daysUntil(item.expiry);
        if (days == null) return "Expiry: " + item.expiry;
        if (days < 0) return "Expired " + Math.abs(days) + " days ago";
        if (days == 0) return "Expires today";
        if (days <= 7) return "Expires in " + days + " days";
        return "Expires " + item.expiry;
    }

    private Long daysUntil(String date) {
        if (date == null || date.trim().isEmpty()) return null;
        try {
            return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date.trim()));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean validDate(String date) {
        return PantryRules.validDate(date);
    }

    private void loadData() {
        pantry.clear();
        shopping.clear();
        try {
            JSONArray items = new JSONArray(prefs.getString("pantry", "[]"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject o = items.getJSONObject(i);
                String name = PantryRules.normalizeName(o.optString("name"));
                String expiry = o.optString("expiry");
                if (!name.isEmpty()) {
                    pantry.add(new PantryItem(name,
                            o.optString("location", "Pantry"),
                            PantryRules.normalizeQuantity(o.optString("quantity")),
                            o.optString("category", "Other"),
                            PantryRules.validDate(expiry) ? expiry : ""));
                }
            }
            JSONArray shops = new JSONArray(prefs.getString("shopping", "[]"));
            for (int i = 0; i < shops.length(); i++) {
                JSONObject o = shops.getJSONObject(i);
                String name = PantryRules.normalizeName(o.optString("name"));
                if (!name.isEmpty() && !PantryRules.containsShopName(shoppingNames(), name)) {
                    shopping.add(new ShopItem(name, o.optBoolean("done")));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveData() {
        try {
            JSONArray items = new JSONArray();
            for (PantryItem item : pantry) {
                JSONObject o = new JSONObject();
                o.put("name", item.name);
                o.put("location", item.location);
                o.put("quantity", item.quantity);
                o.put("category", item.category);
                o.put("expiry", item.expiry);
                items.put(o);
            }
            JSONArray shops = new JSONArray();
            for (ShopItem item : shopping) {
                JSONObject o = new JSONObject();
                o.put("name", item.name);
                o.put("done", item.done);
                shops.put(o);
            }
            prefs.edit().putString("pantry", items.toString()).putString("shopping", shops.toString()).apply();
        } catch (Exception e) {
            setStatus("Save failed: " + e.getMessage());
        }
    }

    private void seedDemoData() {
        pantry.add(new PantryItem("Black beans", "Pantry", "2 cans", "Protein", LocalDate.now().plusDays(90).toString()));
        pantry.add(new PantryItem("Spinach", "Fridge", "1 bag", "Vegetable", LocalDate.now().plusDays(3).toString()));
        pantry.add(new PantryItem("Rice", "Pantry", "1 kg", "Grain", ""));
        shopping.add(new ShopItem("Eggs", false));
        shopping.add(new ShopItem("Greek yogurt", false));
        saveData();
    }

    private void info(String title, String body) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK", null).show();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView title(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(TEXT);
        view.setTextSize(22);
        view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = title(text);
        view.setTextSize(17);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        view.setBackground(rounded(PANEL, LINE, 8));
        view.setElevation(dp(4));
        view.setTranslationZ(dp(1));
        view.setLayoutParams(matchWrap());
        return view;
    }

    private TextView small(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(MUTED);
        return view;
    }

    private View empty(String text) {
        TextView view = small(text);
        view.setPadding(dp(12), dp(14), dp(12), dp(14));
        view.setBackground(rounded(PANEL, LINE, 8));
        view.setElevation(dp(3));
        return view;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setMinHeight(dp(48));
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackground(rounded(GREEN, GREEN, 6));
        button.setElevation(dp(5));
        button.setTranslationZ(dp(1));
        return button;
    }

    private Button secondary(String text) {
        Button button = button(text);
        button.setBackground(rounded(GREEN_DARK, GREEN_DARK, 6));
        return button;
    }

    private EditText textField(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setSingleLine(true);
        edit.setSelectAllOnFocus(true);
        return edit;
    }

    private EditText dateField(String hint) {
        EditText edit = textField(hint);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        edit.setKeyListener(DigitsKeyListener.getInstance("0123456789-"));
        return edit;
    }

    private EditText multiTextField(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setSingleLine(false);
        edit.setMinLines(4);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setSelectAllOnFocus(true);
        return edit;
    }

    private Spinner spinner(String... values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private View labeled(String label, View child) {
        LinearLayout layout = column();
        layout.addView(small(label));
        layout.addView(child, matchWrap());
        return layout;
    }

    private View twoViews(String a, View av, String b, View bv) {
        LinearLayout layout = row();
        LinearLayout left = column();
        LinearLayout right = column();
        left.addView(small(a));
        left.addView(av, matchWrap());
        right.addView(small(b));
        right.addView(bv, matchWrap());
        layout.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, -2, 1);
        rightParams.setMargins(dp(8), 0, 0, 0);
        layout.addView(right, rightParams);
        return layout;
    }

    private View panel(View child) {
        LinearLayout wrap = column();
        wrap.setPadding(0, 0, 0, 0);
        wrap.setBackground(rounded(PANEL, LINE, 8));
        wrap.setElevation(dp(8));
        wrap.setTranslationZ(dp(2));
        wrap.addView(accentStrip(GREEN_DARK), new LinearLayout.LayoutParams(-1, dp(4)));
        LinearLayout inner = column();
        inner.setPadding(dp(14), dp(14), dp(14), dp(14));
        inner.addView(child);
        wrap.addView(inner);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(8), 0, dp(16));
        wrap.setLayoutParams(lp);
        return wrap;
    }

    private View accentStrip(int color) {
        View view = new View(this);
        view.setBackgroundColor(color);
        return view;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(5), 0, dp(10));
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String clean(EditText edit) {
        return edit.getText().toString().trim();
    }

    private String cap(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    private void setStatus(String text) {
        currentStatus = text;
        if (status != null) status.setText(text);
    }

    @Override
    protected void onDestroy() {
        destroyCurrentAdView();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        try {
            if (requestCode == REQ_SCAN_IMAGE) {
                if (data == null) {
                    setStatus("No image was selected.");
                    return;
                }
                Uri uri = data.getData();
                if (uri == null) {
                    setStatus("No image was selected.");
                    return;
                }
                try (InputStream stream = getContentResolver().openInputStream(uri)) {
                    scannerBitmap = BitmapFactory.decodeStream(stream);
                }
                activeTab = 1;
                setStatus("Imported image loaded.");
                processScannerBitmap(scannerBitmap, "imported image");
            } else if (requestCode == REQ_SCAN_CAMERA) {
                if (pendingCameraUri != null) {
                    try (InputStream stream = getContentResolver().openInputStream(pendingCameraUri)) {
                        scannerBitmap = BitmapFactory.decodeStream(stream);
                    }
                    if (scannerBitmap != null) {
                        activeTab = 1;
                        setStatus("Camera image loaded.");
                        processScannerBitmap(scannerBitmap, "camera image");
                    } else {
                        setStatus("Camera image could not be decoded.");
                    }
                } else {
                    setStatus("Camera returned no image.");
                }
            }
        } catch (Exception e) {
            setStatus("Image scan failed: " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        saveData();
        super.onPause();
    }
}
