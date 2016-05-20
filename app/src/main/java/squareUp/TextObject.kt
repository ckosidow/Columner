package squareUp

class TextObject {
    var text: String
    var x: Float = 0.toFloat()
    var y: Float = 0.toFloat()
    var color: FloatArray

    constructor() {
        text = "default"
        x = 0.0f
        y = 0.0f
        color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    }

    constructor(
            text: String,
            xcoord: Float,
            ycoord: Float) {
        this.text = text
        x = xcoord
        y = ycoord
        color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    }
}