package com.khang.photovault.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.khang.photovault.R;
import com.khang.photovault.SlideshowActivity;
import com.khang.photovault.adapter.PhotoAdapter;
import com.khang.photovault.model.Photo;

import java.util.ArrayList;
import java.util.List;

/** Lớp cha chung cho Thư viện + Yêu thích: lưới ảnh, tìm kiếm, sắp xếp, đổi số cột, trình chiếu */
public abstract class BasePhotoFragment extends Fragment {

    protected RecyclerView rv;
    protected PhotoAdapter adapter;
    protected TextView tvEmpty;
    private int span = 3;
    private boolean resumedOnce = false;

    protected abstract boolean favoritesMode();

    protected abstract void loadData();

    /** Gọi khi quay lại màn hình (ví dụ sau khi chụp ảnh / thêm yêu thích) */
    protected void onRefresh() {
        loadData();
    }

    /** Nút chụp ảnh (chỉ Thư viện dùng) */
    protected void onCameraClick() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_photos, c, false);
        rv = v.findViewById(R.id.rvPhotos);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvEmpty.setText(favoritesMode()
                ? "Chưa có ảnh yêu thích nào.\nNhấn giữ một ảnh ở Thư viện để thêm ❤"
                : "Không có ảnh nào trong máy");

        rv.setLayoutManager(new GridLayoutManager(requireContext(), span));
        adapter = new PhotoAdapter(requireActivity(), favoritesMode(), this::loadData);
        rv.setAdapter(adapter);
        rv.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down));

        View fab = v.findViewById(R.id.fabCamera);
        if (favoritesMode()) fab.setVisibility(View.GONE);
        else fab.setOnClickListener(x -> onCameraClick());
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (resumedOnce) onRefresh(); // lần đầu đã load ở onViewCreated
        resumedOnce = true;
    }

    protected void showData(List<Photo> list) {
        adapter.setData(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rv.scheduleLayoutAnimation();
    }

    public void search(String q) {
        if (adapter != null) adapter.setQuery(q);
    }

    public void sort(int type) {
        if (adapter == null) return;
        adapter.setSort(type);
        rv.scheduleLayoutAnimation();
    }

    /** Chuyển giữa lưới 2 cột và 3 cột */
    public void toggleGrid() {
        span = (span == 3) ? 2 : 3;
        ((GridLayoutManager) rv.getLayoutManager()).setSpanCount(span);
        adapter.notifyDataSetChanged();
        rv.scheduleLayoutAnimation();
    }

    /** Mở màn hình trình chiếu với các ảnh đang hiển thị (đã lọc/sắp xếp) */
    public void startSlideshow() {
        List<Photo> shown = adapter.getShown();
        if (shown.isEmpty()) {
            Toast.makeText(getContext(), "Không có ảnh để trình chiếu", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<String> uris = new ArrayList<>();
        for (Photo p : shown) uris.add(p.uri.toString());

        Intent i = new Intent(requireContext(), SlideshowActivity.class);
        i.putStringArrayListExtra("uris", uris);
        i.putExtra("start", 0);
        startActivity(i);
    }
}
