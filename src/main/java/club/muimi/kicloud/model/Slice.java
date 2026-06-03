package club.muimi.kicloud.model;

import lombok.Data;

@Data
public class Slice {
    private int size;
    private String path;
    private boolean done;
}