#!/usr/bin/env python

import roslib; roslib.load_manifest('visualization_marker_tutorials')
from visualization_msgs.msg import Marker, MarkerArray
from geometry_msgs.msg import Point
from lab2.msg import Lines, Single_Line
import rospy
import math

def callback(data):
	global prev_m
	markerArray = MarkerArray()
	m = 0
	length = len(data.line) if len(data.line) > prev_m else prev_m
	for i in xrange(0, length):
		prev_m = len(data.line)
		marker = Marker()
 		marker.header.frame_id = "base_link"
		marker.type = marker.LINE_LIST
		marker.scale.x = 0.05
  		marker.color.a = 1.0
   		marker.color.r = 1.0
  		marker.color.g = 0.0
   		marker.color.b = 0.0
	
		x1 = 0
		x2 = 0
		y1 = 0
		y2 = 0

		if length == len(data.line):
			p = data.line[i]
			x1 = p.x1
			x2 = p.x2
			y1 = p.y1
			y2 = p.y2
			
	
		start = Point()
		start.x = x1
		start.y = y1
		
		end = Point()
		end.x = x2
		end.y = y2
	
		marker.points.append(start)
		marker.points.append(end)
	
		marker.id = m
		m += 1
		markerArray.markers.append(marker)
   	
	publisher.publish(markerArray)

if __name__ == '__main__':
	global publisher, prev_m
	prev_m  = 0
	rospy.init_node('line_check')
	rospy.Subscriber("lines", Lines, callback)
	publisher = rospy.Publisher('visualization_marker_array', MarkerArray)
	r = rospy.Rate(10)
	while not rospy.is_shutdown():
		r.sleep()


