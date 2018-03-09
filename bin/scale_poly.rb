#!/usr/bin/ruby

##
# Scales points given in a two-coordinates per line format (via stdin) to range
# of [0, 500].  Writes result to stdout.
#
SCALE = 500

require 'rational'

xs = []
ys = []

min_x = nil
max_x = nil
min_y = nil
max_y = nil

cnt = 0
ARGF.each do |line|
  cnt += 1
  line = line.gsub(/#.*$/, '').strip
  next if line == ''

  strs = line.strip.split.map(&:strip)
  if strs.length != 2
    STDERR.puts "Bad input line #{cnt}"
    exit 1
  end

  x = Rational(strs[0])
  y = Rational(strs[1])

  min_x = if !min_x then x else [min_x, x].min end
  min_y = if !min_y then y else [min_y, y].min end
  max_x = if !max_x then x else [max_x, x].max end
  max_y = if !max_y then y else [max_y, y].max end

  xs += [x]
  ys += [y]
end

# width_min = [max_x - min_x, max_y - min_y].min
width = [max_x - min_x, max_y - min_y].max

xs.map!{|x| SCALE*(x - min_x)/width}
ys.map!{|y| SCALE*(y - min_y)/width}

xs.zip(ys).each do |pt|
  puts pt.map(&:to_f).join(" ")
end
