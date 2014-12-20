#include	"gprot.h"

int main(int argc,char *argv[])
{
	ros::init(argc,argv,"bug2");
	NodeHandle n;
	islop = (g_endPos.y - g_startPos.y)/(g_endPos.x - g_startPos.x);
	vel = n.advertise<geometry_msgs::Twist>("cmd_vel",1);
	Rate loop_rate(10);
	Subscriber sub = n.subscribe("base_pose_ground_truth",1,basePoseSubCallback);
	Subscriber sub1 = n.subscribe("base_scan",1,LaserSubCallback);
	bug2_algorithm(loop_rate);
	spin();
	return 0;
}
void bug2_algorithm(Rate loop_rate)
{
	STATE state = GOAL_SEEK;
	while(ros::ok() && state != AT_GOAL)
	{
		float dist = goalDistance();
		if(GOAL_THRESHOLD > dist)
		{
			ROS_INFO("At goal\n");
			stopRobot();
			state = AT_GOAL;
		}
		else
		{
			float fwdVel = g_speed;
			float rotVel;
			if(state == GOAL_SEEK)
			{
				ROS_INFO("goal seek");
			 	rotVel = getGoalSeekRotation();
				if(g_obstacleDetected)
					state = WALL_FOLLOW;
			}
			else if(state == WALL_FOLLOW)
			{
				ROS_INFO("wall follow");
				rotVel = turn;
				
		ROS_INFO("turn : %f",turn);
				if( abs(islop - fslop) < 0.1 && range > WALL_THRESHOLD)
					state = GOAL_SEEK;
			}	
			setVelocoty(fwdVel,rotVel);
		}
		spinOnce();
		loop_rate.sleep();
	}
	if(state == AT_GOAL)
		ROS_INFO("I have arrived at Goal Already");
}
void basePoseSubCallback(const Odometry msg)
{
	double x =g_robotPos.x = msg.pose.pose.position.x;
	double y =g_robotPos.y = msg.pose.pose.position.y;
	fslop = (g_endPos.y - g_robotPos.y)/(g_endPos.x - g_robotPos.x);
	x = msg.pose.pose.orientation.x;
	y = msg.pose.pose.orientation.y;
	double z = msg.pose.pose.orientation.z;
	double w = msg.pose.pose.orientation.w;
	tf::Quaternion q(msg.pose.pose.orientation.x,msg.pose.pose.orientation.y,
					-msg.pose.pose.orientation.z,-msg.pose.pose.orientation.w);
	double yaw,pitch,roll;
	tf::Matrix3x3(q).getRPY(yaw,pitch,roll);
	robotAngle = roll;
}
void LaserSubCallback(const LaserScan scan)
{
	int i=0,size = scan.ranges.size();
	int mid = size/2;
	minimum =range= MAX;
	for(i=mid-90;i<mid+90;i++)
	{
		if(scan.ranges[i]  <=  range)
			range = scan.ranges[i];
	}
	if(range < OBSTACLE_THRESHOLD)
		g_obstacleDetected = 1;
	else 
		g_obstacleDetected = 0;
	for(i=mid-180;i<mid+180;i++)
	{
		if(scan.ranges[i]  <=  minimum)
			minimum = scan.ranges[i];
	}	
	if(minimum <=  GOAL_THRESHOLD)
	{			
		ROS_INFO("i am about to reach at Goal");
		g_obstacleDetected = 0;
	}
	else if(minimum <= OBSTACLE_THRESHOLD)
	{
		turn = -(i*PI)/180.0;
		if(range < 0.5 || abs(minimum-scan.ranges[360]) > 0.2 ) turn = turn/10;
		else  if(abs(minimum-scan.ranges[360]) > 0.1) turn = turn/20;
		else turn = 0; 
	}
	else 
	{
		turn = i*PI/180;
		turn = turn/10;
	}
	if(!g_obstacleDetected && g_speed < NORMAL_SPEED )
		g_speed += 0.05;
	else if(g_obstacleDetected)
		 g_speed = SLOW_WALK;
}
float goalDistance()
{
	return sqrt( pow(g_endPos.x - g_robotPos.x ,2) + pow(g_endPos.y - g_robotPos.y ,2));
}
float goalAngle()
{
	float y = g_endPos.y - g_robotPos.y;
	float x = g_endPos.x - g_robotPos.x;
	return (float)(atan2(y,x));
}
void stopRobot()
{
	pos.linear.x = 0;
	pos.linear.y = 0;
	pos.angular.x = 0;
	pos.angular.z = 0;
	vel.publish(pos);
}
void setVelocoty(float fwdVel,float rotVel)
{
	pos.linear.x = fwdVel;
	pos.angular.z = rotVel;
	vel.publish(pos);
}
float getGoalSeekRotation()
{
	float angle = goalAngle() - robotAngle;
	if( abs(angle) < 0.01)
		return 0;
	else
		return angle;
}
