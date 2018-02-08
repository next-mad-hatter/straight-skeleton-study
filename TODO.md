* Bug: found one (simple) instance which crashes the program -- is this due to
  a bug or violated instance constraint (e.g. general position or some such)?

* Bug: dragging mouse inside a built polygon in the main canvas pane sometimes
  yields a null pointer exception (not sure yet when this triggers -- maybe if
  we start close enough to a vertex?)

* Bug: Polygon generating program writes those as xml files while skeleton part
  reads/writes plain text.  A workaround right now would be to load the polygon
  into main app by closing the generating app window.

* Add current dir as default path in open/save dialogs, keep track of last path
  used in both (currently only open dialog does this iianm)

* Library interface

* Count events (flip, split etc, extraordinary = flip not changing angle
  category?)

* Generate large instances & measure performance on those

* Include a random generation alternative?

* Zoom & autozoom at load

* History: back steps

* Straight jump to result

* Large weights -> animation sometimes gets too slow at some point

* Adjustable animation speed, animations switchable

* Enable comparison of skeletons when changing weights / moving vertex

* Get rid of dependencies remaining local (randomPolygon & poly2tri)
  or replace libs in question by something more robust

* Simplicity test (e.g. https://www.webcitation.org/6ahkPQIsN) or
  alt. triangulation (e.g.  https://github.com/gwlucastrig/Tinfour
  or https://parasol.tamu.edu/publications/abstract.php?pub_id=185)?

* Alternative (fx/fn-fx/re-frame etc) ui if changes extensive?

* Investigate numerical stability

* Create a useful readme?

