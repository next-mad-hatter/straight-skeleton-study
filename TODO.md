* Use issue tracker?

* Set up consistant indenting & os-appropriate line breaks (wtf spacemacs?!)

* Count events (flip, split etc, extraordinary = flip not changing angle
  category?)

* Show triangulation as very first step

* History: back steps

* Straight jump to result

* Zoom; autozoom & autopan at load

* Separate points coordinates and drawn points' coordinates, automatically compute good mappings

* Adjustable animation speed, animations switchable & pausable

* Enable comparison of skeletons when changing weights / moving vertex

* Large weights can result in animation becoming very slow

* Area not originally shown in canvas doesn't get background painted when canvas is scrolled

* Get confirmation from user before overwriting files

* Separate algorithm form the applet

* Extend the set of correctness tests

* Bug(s): found number of instances which yield incorrect behaviour -> check e.g. behaviour at coinsiding events

* Bug: on one specific instance (simple-07 as of now) correct skeleton is
  computed when run via noswingworker (single/batch) variant while the applet
  version produces bad result

* Bug: ConcurrentModificationException(s) encountered when drawing in GraphicPanel.paintMovedPoints

* Bug: dragging mouse inside a built polygon in the main canvas pane sometimes
  yields a null pointer exception (not sure yet when this triggers -- maybe if
  we start close enough to a vertex?)

* Bug: sometimes (loading invalid file?) FileHandler.openPoly barfs a "File cannot be null" exception

* Bug: Polygon generating program writes those as xml files while skeleton part
  reads/writes plain text.  A workaround right now would be to load the polygon
  into main app by closing the generating app window.

* Batch runner: read multiple batch files & add output prefix option -> shell batch runner

* Add loaded file name to window title

* Get rid of bad/hardcoded resource paths

* Get rid of dependencies remaining local (randomPolygon & poly2tri)
  or replace libs in question by something more robust

* Simplicity test (e.g. https://www.webcitation.org/6ahkPQIsN) or
  alt. triangulation (see e.g.
      https://github.com/orbisgis/jdelaunay/wiki
      https://github.com/gwlucastrig/Tinfour
   or https://parasol.tamu.edu/publications/abstract.php?pub_id=185
  )?

* Alternative (fx/tornado/fn-fx/re-frame etc) ui if changes extensive?

* Investigate numerical stability

* Investigate rendering artefacts

* Check source for appropriate line breaks (shouldn't git have converted those? O_x)

