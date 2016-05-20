package squareUp

var sp_Image: Int = 0
var sp_Text: Int = 0

val vs_Image =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 a_texCoord;" +
                "varying vec2 v_texCoord;" +
                "void main() {" +
                " gl_Position = uMVPMatrix * vPosition;" +
                " v_texCoord = a_texCoord;" +
                "}"
val fs_Image =
        "precision mediump float;" +
                "varying vec2 v_texCoord;" +
                "uniform sampler2D s_texture;" +
                "void main() {" +
                " gl_FragColor = texture2D( s_texture, v_texCoord );" +
                "}"
val vs_Text =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec4 a_Color;" +
                "attribute vec2 a_texCoord;" +
                "varying vec4 v_Color;" +
                "varying vec2 v_texCoord;" +
                "void main() {" +
                " gl_Position = uMVPMatrix * vPosition;" +
                " v_texCoord = a_texCoord;" +
                " v_Color = a_Color;" +
                "}"
val fs_Text =
        "precision mediump float;" +
                "varying vec4 v_Color;" +
                "varying vec2 v_texCoord;" +
                "uniform sampler2D s_texture;" +
                "void main() {" +
                " gl_FragColor = texture2D( s_texture, v_texCoord ) * v_Color;" +
                " gl_FragColor.rgb *= v_Color.a;" +
                "}"