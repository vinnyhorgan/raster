# OpenGL 1.1 Ultimate Specification for Modern Re-implementation

This document is a high-fidelity extraction of the OpenGL 1.1 Specification (1997), designed for an LLM agent to build a bit-perfect emulation layer on top of modern APIs like OpenGL 4.6, Vulkan, or DX12.

---

## 1. Core Data Types & Conversions
All integer inputs must be converted to internal 32-bit float representations before processing.

| GL Type | Conversion to Float ($f$) |
| :--- | :--- |
| `ubyte` | $c / (2^8 - 1)$ |
| `byte` | $(2c + 1) / (2^8 - 1)$ |
| `ushort` | $c / (2^{16} - 1)$ |
| `short` | $(2c + 1) / (2^{16} - 1)$ |
| `uint` | $c / (2^{32} - 1)$ |
| `int` | $(2c + 1) / (2^{32} - 1)$ |

---

## 2. Complete API Command Reference

### 2.1 Primitive Specification
*   **Structure:** `glBegin(mode)`, `glVertex*`, `glColor*`, `glIndex*`, `glNormal*`, `glTexCoord*`, `glEdgeFlag*`, `glEvalCoord*`, `glEvalPoint*`, `glMaterial*`, `glCallList*`, `glEnd()`.
*   **Modes:** `POINTS`, `LINES`, `LINE_STRIP`, `LINE_LOOP`, `TRIANGLES`, `TRIANGLE_STRIP`, `TRIANGLE_FAN`, `QUADS`, `QUAD_STRIP`, `POLYGON`.
*   **Coordinate variants:** Supports 2, 3, and 4-component versions across `short`, `int`, `float`, `double` (e.g., `glVertex2i`, `glVertex4dv`).

### 2.2 Matrix & Transform State
*   `glMatrixMode(enum mode)`: `MODELVIEW`, `PROJECTION`, `TEXTURE`.
*   `glLoadIdentity()`, `glLoadMatrix{fd}(m)`, `glMultMatrix{fd}(m)`.
*   `glRotate{fd}(angle, x, y, z)`, `glTranslate{fd}(x, y, z)`, `glScale{fd}(x, y, z)`.
*   `glFrustum`, `glOrtho`, `glPushMatrix`, `glPopMatrix`.
*   **Stacks:** `MODELVIEW` (min 32 deep), `PROJECTION` (min 2), `TEXTURE` (min 2).

### 2.3 Lighting & Materials (The Equations)
The vertex color $c$ is calculated as:
$c = e_{cm} + a_{cm}a_{cs} + \sum_{i=0}^{n-1} (att_i)(spot_i) [a_{cm}a_{cli} + (n \cdot L_i)d_{cm}d_{cli} + (n \cdot h_i)^{s_{rm}}s_{cm}s_{cli}]$

*   `glLight{if}[v](light, pname, params)`: `AMBIENT`, `DIFFUSE`, `SPECULAR`, `POSITION`, `SPOT_DIRECTION`, `SPOT_EXPONENT`, `SPOT_CUTOFF`, `CONSTANT_ATTENUATION`, `LINEAR_ATTENUATION`, `QUADRATIC_ATTENUATION`.
*   `glMaterial{if}[v](face, pname, params)`: `AMBIENT`, `DIFFUSE`, `SPECULAR`, `EMISSION`, `SHININESS`, `AMBIENT_AND_DIFFUSE`, `COLOR_INDEXES`.
*   `glLightModel{if}[v](pname, params)`: `LIGHT_MODEL_AMBIENT`, `LIGHT_MODEL_LOCAL_VIEWER`, `LIGHT_MODEL_TWO_SIDE`.

### 2.4 Texturing & Objects
*   `glTexImage1D`, `glTexImage2D`, `glTexSubImage1D`, `glTexSubImage2D`.
*   `glCopyTexImage1D`, `glCopyTexImage2D`, `glCopyTexSubImage1D`, `glCopyTexSubImage2D`.
*   `glBindTexture`, `glGenTextures`, `glDeleteTextures`, `glPrioritizeTextures`, `glAreTexturesResident`.
*   `glTexParameter{if}[v]`: `MIN_FILTER`, `MAG_FILTER`, `WRAP_S`, `WRAP_T`, `BORDER_COLOR`, `PRIORITY`.
*   `glTexEnv{if}[v]`: `MODE` (`MODULATE`, `DECAL`, `BLEND`, `REPLACE`), `COLOR`.
*   `glTexGen{if}[v]`: `MODE` (`OBJECT_LINEAR`, `EYE_LINEAR`, `SPHERE_MAP`), `OBJECT_PLANE`, `EYE_PLANE`.

### 2.5 Pixel & Raster Operations
*   `glPixelStore{if}`, `glPixelTransfer{if}`, `glPixelMap{ui us f}v`, `glPixelZoom`.
*   `glReadPixels`, `glDrawPixels`, `glCopyPixels`, `glBitmap`.
*   `glScissor`, `glAlphaFunc`, `glStencilFunc`, `glStencilOp`, `glDepthFunc`, `glBlendFunc`, `glLogicOp`.
*   `glCullFace`, `glFrontFace`, `glPointSize`, `glLineWidth`, `glLineStipple`, `glPolygonMode`, `glPolygonStipple`, `glPolygonOffset`.

### 2.6 Evaluators (NURBS/B-Splines Basis)
*   `glMap1{fd}`, `glMap2{fd}`, `glMapGrid1{fd}`, `glMapGrid2{fd}`.
*   `glEvalCoord1{fd}[v]`, `glEvalCoord2{fd}[v]`, `glEvalMesh1`, `glEvalMesh2`, `glEvalPoint1`, `glEvalPoint2`.

### 2.7 Display Lists
*   `glNewList`, `glEndList`, `glCallList`, `glCallLists`, `glGenLists`, `glDeleteLists`, `glIsList`, `glListBase`.

---

## 3. Initial State Values (The Implementation Blueprint)

| State Variable | Initial Value |
| :--- | :--- |
| `CURRENT_COLOR` | (1, 1, 1, 1) |
| `CURRENT_NORMAL` | (0, 0, 1) |
| `MATRIX_MODE` | `MODELVIEW` |
| `SHADE_MODEL` | `SMOOTH` |
| `DEPTH_TEST` | `FALSE` (Disabled) |
| `DEPTH_FUNC` | `LESS` |
| `BLEND` | `FALSE` |
| `BLEND_SRC` | `ONE` |
| `BLEND_DST` | `ZERO` |
| `CULL_FACE` | `FALSE` |
| `CULL_FACE_MODE` | `BACK` |
| `FRONT_FACE` | `CCW` |
| `LIGHTING` | `FALSE` |
| `LIGHTi` | `FALSE` (All 8+ lights) |
| `TEXTURE_1D/2D` | `FALSE` |
| `TEXTURE_ENV_MODE` | `MODULATE` |

---

## 4. Re-implementation Logic for Modern GL (4.6)

### 4.1 Global State Object
Create a `GL11Context` struct containing all variables from the "State Variable Tables" (Chapter 6).
*   **Dirty Flags:** Use a bitmask to track which states changed. Before any draw call, update the "Uber-Shader" uniforms.

### 4.2 Immediate Mode (The "Ring Buffer" Strategy)
1.  Allocate a 16MB persistent-mapped Buffer Storage (`glBufferStorage`).
2.  `glBegin`: Store the starting offset.
3.  `glVertex`: Write interleaved data (Pos, Color, Norm, Tex) directly to the buffer.
4.  `glEnd`: Execute `glDrawArrays` on the range.
5.  **Auto-Flush:** If the buffer fills, flush and wrap around.

### 4.3 Fixed-Function Uber-Shader
Write a single GLSL shader that emulates the entire pipeline:
*   **Vertex Stage:**
    *   Manual transformation: `gl_Position = u_Projection * u_ModelView * vec4(a_Pos, 1.0)`.
    *   Lighting loop: Iterate through `u_Lights[8]` and apply the formula in section 2.3.
    *   TexGen: Implement `ObjectLinear` and `SphereMap` math in-shader.
*   **Fragment Stage:**
    *   Texture modulation: `fragColor = texColor * vertexColor`.
    *   Fog: `mix(fragColor, u_FogColor, fogFactor)`.
    *   Alpha Test: `if (color.a < u_AlphaRef) discard;`.

### 4.4 Matrix Stack
Do NOT use `glm::stack`. Implement the matrix stack manually to match the spec's behavior for `glPushMatrix` and `glPopMatrix` error conditions (Stack Overflow/Underflow).

---

## 5. Implementation-Dependent Limits (Minimums)
*   `MAX_LIGHTS`: 8
*   `MAX_CLIP_PLANES`: 6
*   `MAX_MODELVIEW_STACK_DEPTH`: 32
*   `MAX_PROJECTION_STACK_DEPTH`: 2
*   `MAX_TEXTURE_STACK_DEPTH`: 2
*   `MAX_NAME_STACK_DEPTH`: 64
*   `MAX_LIST_NESTING`: 64
*   `MAX_VIEWPORT_DIMS`: [At least window size]
*   `MAX_ATTRIB_STACK_DEPTH`: 16

---

## 6. Error Codes to Emulate
*   `GL_INVALID_ENUM`: Parameter out of range.
*   `GL_INVALID_VALUE`: Numeric argument out of range.
*   `GL_INVALID_OPERATION`: Command issued in invalid state (e.g., `glMatrixMode` inside `glBegin`).
*   `GL_STACK_OVERFLOW/UNDERFLOW`: Matrix or Attribute stack limits reached.
*   `GL_OUT_OF_MEMORY`: Allocation failed.
