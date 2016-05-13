package columner

val PROTRUSION_HEIGHT = 0.025f

val PLAYER_HEIGHT = 0.075f

// Program variables
var sp_Image: Int = 0
var sp_Text: Int = 0
/* SHADER Image
   *
   * This shader is for rendering 2D images straight from a texture
   * No additional effects.
   *
   */
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
/* SHADER Text
  *
  * This shader is for rendering 2D text textures straight from a texture
  * Color and alpha blended.
  *
  */
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