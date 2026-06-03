package club.muimi.kicloud.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;

public class Slices {
    private final Map<Integer, Slice> slicesMap;
    private final int count;
    @Getter
    private final String fullFileMD5;
    @Getter
    private final Long ownerId;
    @Getter
    private final LocalDateTime createdAt = LocalDateTime.now();
    @Getter
    private LocalDateTime updatedAt;

    public Slices(int sliceCount, String fullFileMD5, Long ownerId) {
        this.slicesMap = new HashMap<>();
        for (int i = 0; i < sliceCount; i++) {
            slicesMap.put(i, new Slice());
        }
        this.count = sliceCount;
        this.fullFileMD5 = fullFileMD5;
        this.ownerId = ownerId;
        updatedAt = createdAt;
    }

    public void addSlice(int sliceNum, int size, String path) {
        if (sliceNum >= count) return;
        Slice slice = slicesMap.get(sliceNum);
        if (slice == null) slice = new Slice();
        if (slice.isDone()) return; // 已经完成的切片不再处理
        slice.setSize(size);
        slice.setPath(path);
        slice.setDone(true);
        slicesMap.put(sliceNum, slice);
        updatedAt = LocalDateTime.now();
    }

    public boolean isAllDone() {
        for (Slice slice : slicesMap.values()) {
            if (!slice.isDone()) return false;
        }
        return true;
    }

    public List<Slice> getSlices() {
        List<Slice> slices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            slices.add(slicesMap.get(i));
        }
        return slices;
    }
}