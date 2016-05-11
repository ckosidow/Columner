package columner;

public class TextObject {
    public String text;
    public float x;
    public float y;
    public float[] color;

    public TextObject() {
        text = "default";
        x = 0.0f;
        y = 0.0f;
        color = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    }

    public TextObject(
            final String text,
            final float xcoord,
            final float ycoord
    ) {
        this.text = text;
        x = xcoord;
        y = ycoord;
        color = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    }
}