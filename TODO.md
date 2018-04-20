* Use issue tracker?

* Keeps using CPU after computation complete (again)

* Check large polygons set results

* Ad input scaling: implement
    - scaling for formats other than simple coordinates list
    - scaling range setting
    - input scaling setting for applet
    - per entry setting in batch run

* Result checker: check if set of all vertices is equal to set of all tree leafs?

* Debug + test + cleanup zoom & history

* History: test back steps, better controls (usability/stability)

* Zoom: autozoom & autopan at load / manually triggered apart from that?

* Zoom in on area

* Straight jump to result

* History animation?

* Enable saving skeleton to a file from applet

* Count different kinds of flip events

* Adjustable animation speed, animations switchable & pausable

* Enable comparison of skeletons when changing weights / moving vertex

* Large weights can result in animation becoming very slow

* Area not originally shown in canvas doesn't get background painted when canvas is scrolled

* Get confirmation from user before overwriting files

* Separate algorithm from the applet

* Bug(s): numerous instances yield incorrect results and/or crash

* Bug(s): sometimes duplicate edges are present in computed skeleton
  (currently e.g. misc/crashes.[4-6])

* Bug: when using the applet, recomputing skeleton a second time can (e.g. with
  crashes.2.min) yield incorrect skeleton

* Bug(s)/numerical issues: in some instances (currently, e.g. simple-02)
  some very short edges are inserted

* Bug(s): some instances compute correctly when run via applet only and vice versa
  (currently e.g. simple-07 and simple-18)

* Bug: Some polygons yield ConcurrentModificationException (e.g. misc/snail.01)

* Bug: ConcurrentModificationException(s) encountered when drawing in GraphicPanel.paintMovedPoints

* Bug: dragging mouse inside a built polygon in the main canvas pane sometimes
  yields a null pointer exception (not sure yet when this triggers -- maybe if
  we start close enough to a vertex?)

* Bug: polygon generating program writes those as xml files while skeleton part
  reads/writes plain text.  Loading former always crashes on my box -- a
  workaround right now would be to load the polygon into main app by closing
  the generating app window.

* Bug: when loading invalid file, FileHandler.openPoly barfs a "File cannot be null" exception

* Whenever dragging a vertex results in nonsimple polygon, the app barfs

* Batch runner: read multiple batch files & add output prefix option -> shell batch runner

* Get rid of bad/hardcoded resource paths ('/images/')

* Get rid of dependencies remaining local (randomPolygon & poly2tri)
  or replace libs in question by something more robust

* Simplicity test (e.g. https://www.webcitation.org/6ahkPQIsN) or
  alt. triangulation (see e.g.
      https://github.com/orbisgis/jdelaunay/wiki
      https://github.com/gwlucastrig/Tinfour
   or https://parasol.tamu.edu/publications/abstract.php?pub_id=185
  )?

* Alternative (fx/tornado/fn-fx/re-frame etc) ui if changes extensive?

* Investigate numerical stability (see very short arcs example); further
  research: can we use e.g. apache commons math library to implement exact
  computations (if this is algebraically feasible at all)?


* Investigate rendering artefacts

* Extend the set of correctness tests

* Set up consistant indenting & os-appropriate line breaks (wtf spacemacs?!)

* Apply license in filehandler to prior code only

