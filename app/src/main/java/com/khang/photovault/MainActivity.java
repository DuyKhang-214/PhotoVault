package com.khang.photovault;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.khang.photovault.fragments.BasePhotoFragment;
import com.khang.photovault.fragments.FavoritesFragment;
import com.khang.photovault.fragments.GalleryFragment;
import com.khang.photovault.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            Fragment f;
            String title;
            int id = item.getItemId();
            if (id == R.id.nav_gallery) {
                f = new GalleryFragment();
                title = "Thư viện";
            } else if (id == R.id.nav_favorites) {
                f = new FavoritesFragment();
                title = "Yêu thích";
            } else {
                f = new ProfileFragment();
                title = "Hồ sơ";
            }
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.container, f)
                    .commitNow();
            toolbar.setTitle(title);
            invalidateOptionsMenu(); // cập nhật hiện/ẩn các nút trên toolbar
            return true;
        });

        if (b == null) nav.setSelectedItemId(R.id.nav_gallery);
    }

    /** Fragment ảnh hiện tại (null nếu đang ở tab Hồ sơ) */
    private BasePhotoFragment current() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        return f instanceof BasePhotoFragment ? (BasePhotoFragment) f : null;
    }

    // ===== Options Menu =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchView sv = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (sv != null) {
            sv.setQueryHint("Tìm ảnh theo tên...");
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String q) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String q) {
                    BasePhotoFragment f = current();
                    if (f != null) f.search(q);
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean show = current() != null;
        menu.findItem(R.id.action_search).setVisible(show);
        menu.findItem(R.id.action_sort).setVisible(show);
        menu.findItem(R.id.action_grid).setVisible(show);
        menu.findItem(R.id.action_slideshow).setVisible(show);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        BasePhotoFragment f = current();

        if (id == R.id.sort_newest) { if (f != null) f.sort(0); return true; }
        if (id == R.id.sort_oldest) { if (f != null) f.sort(1); return true; }
        if (id == R.id.sort_name)   { if (f != null) f.sort(2); return true; }
        if (id == R.id.action_grid) { if (f != null) f.toggleGrid(); return true; }
        if (id == R.id.action_slideshow) { if (f != null) f.startSlideshow(); return true; }
        if (id == R.id.action_logout) {
            ProfileFragment.logout(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
