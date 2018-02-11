* Use issue tracker?

* Bug(s): found number of instances which yield incorrect behaviour -> check e.g. behaviour at coinsiding events

* Factor out library part & split off the app and single file runner

* Zoom & autozoom at load

* Count events (flip, split etc, extraordinary = flip not changing angle
  category?)

* Compile a set of correctness tests

* Bug: ConcurrentModificationException(s) encountered when drawing in GraphicPanel.paintMovedPoints

* Bug: dragging mouse inside a built polygon in the main canvas pane sometimes
  yields a null pointer exception (not sure yet when this triggers -- maybe if
  we start close enough to a vertex?)

* Generate large instances & measure performance on those

* History: back steps

* Straight jump to result

* Include a random generation alternative?

* Show triangulation as very first step

* Pause animation

* Large weights -> animation sometimes gets too slow at some point

* Adjustable animation speed, animations switchable

* Enable comparison of skeletons when changing weights / moving vertex

* Get confirmation from user before overwriting files

* Bug: sometimes (loading invalid file?) FileHandler.openPoly barfs a "File cannot be null" exception

* Bug: Polygon generating program writes those as xml files while skeleton part
  reads/writes plain text.  A workaround right now would be to load the polygon
  into main app by closing the generating app window.

* Get rid of dependencies remaining local (randomPolygon & poly2tri)
  or replace libs in question by something more robust

* Simplicity test (e.g. https://www.webcitation.org/6ahkPQIsN) or
  alt. triangulation (e.g.  https://github.com/gwlucastrig/Tinfour
  or https://parasol.tamu.edu/publications/abstract.php?pub_id=185)?

* Alternative (fx/fn-fx/re-frame etc) ui if changes extensive?

* Investigate numerical stability

* Investigate rendering artefacts

* Create a useful readme?

* Check source for appropriate line breaks (shouldn't git have converted those? O_x)
