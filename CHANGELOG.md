* Added context switching to triton controller (multiple editable & switchable skeletons with browsable history each)
* Added kotton trace export from triton & campskeleton
* Added kotton wrapper with campskeleton integration
* Added timeout to runner call
* Added loaded file name to window title
* Added optional input scaling for coordinate list inputs & compressed svg output
* Added autozoom functionality
* Implemented basic browsing back through events history
* Applet: show triangulation as first step
* Singe/batch runners count events, remove superfluous edges (source unknown as of now) & check tree structure of the results
* Polygon loading parser: ordering bugs fixed
* Added a batch runner & a set of small test polygons
* Implemented loading plaintext/xz-compressed coordinates lists
* Added autonomous single file runner
* Added some simple polygons which yield incorrect behaviour
* Got rid of some linter warnings (some redundand casts, raw types and unchecked casts eliminated)
* Added current dir as default path in open/save dialogs
* Fixed the cpu hogging bug caused by a cycle (i.e. bottomless recursion) in render updates
* Brought sources into a buildable state
