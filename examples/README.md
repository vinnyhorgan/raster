# Raster GL Examples

Each folder is a standalone Raster project. Run one with:

```sh
mvn process-classes exec:exec -Dexec.args="examples/01-primitives"
```

The examples intentionally cover the currently implemented `rs.gl` foundation:
immediate-mode primitives, colors, matrix transforms, matrix stacks, viewport,
blending, depth testing, culling, point/line sizing, and explicit matrix upload.

## Examples

- `01-primitives`: points, lines, strips, loops, triangles, fans, quads, quad
  strips, and polygons.
- `02-matrix-stack`: nested transforms with push/pop matrix behavior.
- `03-blending`: alpha blending and draw ordering.
- `04-depth-culling`: perspective projection, depth test, and face culling.
- `05-viewports`: multiple viewport renders in one frame.
- `06-load-matrix`: `loadMatrix` and `multMatrix` with raw 4x4 Lua tables.
- `07-lines-points`: point size, line width, line loops, and animated line
  strips.
- `08-stress-grid`: many immediate-mode quads with animated colors.
- `09-debug-text`: `rs.debug.print` text overlay rendered on top of GL output.

