#!/usr/bin/ruby

##
# A shortcut to create batches from file lists.
#

PREFIX="/tmp/straight_skeleton/out/"

ARGF.each do |file|
  file = file.strip
  name = File.basename(file, '.dat.xz')
  puts [
    file,
    PREFIX+name+".skel",
    PREFIX+name+".stat",
    PREFIX+name+".svg.gz",
  ].join(" ")
end

