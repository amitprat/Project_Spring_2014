#include "ros/ros.h"
#include <sensor_msgs/LaserScan.h>
#include <geometry_msgs/Twist.h>
#include <math.h>
#include "std_msgs/String.h"
using namespace geometry_msgs;
using namespace sensor_msgs;
using namespace ros;
using namespace std_msgs;
#define TOLERANCE 0.000001
#define MAX 999
#define DETECTION_DIST 0.5
float g_start_speed = 0.1;
int g_status = 1;
float minimum = MAX;
float sideLength = 1;
typedef enum{ FWD ,LEFT,REV ,RIGHT}DIR;
DIR dir1 = FWD;int dir = dir1;
void LaserSubCallback(const LaserScan scan);
int walk_straight(Twist pos,Publisher vel,Rate loop_rate);
int walk_circle(Twist pos,Publisher vel,Rate loop_rate);
int walk_square(Twist pos,Publisher vel,Rate loop_rate);
int main(int argc,char *argv[])
{
	ros::init(argc,argv,"MoveRobot");
	NodeHandle n;
	Twist pos;
	Publisher vel = n.advertise<geometry_msgs::Twist>("cmd_vel",1);
	Rate loop_rate(10);
	Subscriber sub = n.subscribe("base_scan",1,LaserSubCallback);
	walk_straight(pos,vel,loop_rate);
	//walk_circle(pos,vel,loop_rate);
	//walk_square(pos,vel,loop_rate);
	spin();
	return 0;
}
void LaserSubCallback(const LaserScan scan)
{
	int i=0,size = scan.ranges.size();
	ROS_INFO("size : %d",size);
	minimum = MAX;
	for(i=0;i<size;i++)
	{
		if(scan.ranges[i]  <=  minimum)
			minimum = scan.ranges[i];
	}
	ROS_INFO("min distance : %f",minimum);
	if(minimum <=  TOLERANCE)			
		ROS_INFO("i have collided already!!");
	else if(minimum <= DETECTION_DIST)
	{
		ROS_INFO("i am at most 50 cm away from obstacle : %f",scan.ranges[i]);
		g_status = 0;
	}
	else if(minimum	<= 1)
	{
		g_start_speed = minimum - DETECTION_DIST;
	}
	else 
		g_start_speed = 1;
}
int walk_straight(Twist pos,Publisher vel,Rate loop_rate)
{
	while(ros::ok())
	{
		if(g_status == 0)
		{		
	       	    ROS_INFO("I am Stopped Now!!");
		    pos.linear.x = 0;
		    vel.publish(pos);
		}
		else
		{
			ROS_INFO("i am moving forward");
			pos.linear.x = g_start_speed;
			vel.publish(pos);
		}	
		spinOnce();
		loop_rate.sleep();
	}
	return 0;
}
int walk_circle(Twist pos,Publisher vel,Rate loop_rate)
{
	while(ros::ok())
	{
		if(g_status == 0)
		{		
	       	    	ROS_INFO("I am Stopped Now!!");
		    	pos.angular.x = 0;
		    	pos.angular.z = 0;
		    	vel.publish(pos);
		}
		else
		{
			ROS_INFO("i am moving forward");
			pos.linear.x = g_start_speed;
			pos.angular.z = g_start_speed;
			vel.publish(pos);
		}	
		spinOnce();
		loop_rate.sleep();
	}
	return 0;
}
int walk_square(Twist pos,Publisher vel,Rate loop_rate)
{
	while(ros::ok())
	{
		if(g_status == 0)
		{		
	       	    	ROS_INFO("I am Stopped Now!!");
		    	pos.angular.x = 0;
		    	pos.angular.y = 0;
		    	vel.publish(pos);
		}
		else
		{
			ROS_INFO("i am moving forward");
			if(g_start_speed > sideLength)
				g_start_speed = sideLength;
			if(g_start_speed)
			{
				switch(dir)
				{
					case FWD:
						pos.linear.x = g_start_speed;
						break;
					case LEFT:
						pos.linear.y = -g_start_speed;
						break;
					case REV:
						pos.linear.x = -g_start_speed;
						break;
					case RIGHT:
						pos.linear.y = g_start_speed;
						break;
				
				}
				sideLength -=g_start_speed;
				vel.publish(pos);
			}
			else
			{
				dir = (dir+1)%4;
				sideLength = 1;
				
			}
		}	
		spinOnce();
		loop_rate.sleep();
	}
	return 0;
}
