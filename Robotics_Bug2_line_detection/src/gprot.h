#include 	"ros/ros.h"
#include 	<sensor_msgs/LaserScan.h>
#include 	<geometry_msgs/Twist.h>
#include 	<math.h>
#include 	"std_msgs/String.h"
#include 	<tf/transform_datatypes.h>
#include 	<nav_msgs/Odometry.h>
#include 	<tf/transform_broadcaster.h>
#include 	<math.h>
#include 	"bullet/LinearMath/btMatrix3x3.h"
#include	<vector>
#include 	<nav_msgs/Odometry.h>
#include 	<visualization_msgs/Marker.h>
#include 	<visualization_msgs/MarkerArray.h>

using namespace nav_msgs;
using namespace geometry_msgs;
using namespace sensor_msgs;
using namespace ros;
using namespace std_msgs;
using namespace std;

/*Bug 2 Declarations */
#define TOLERANCE 0.001
#define GOAL_THRESHOLD 0.1
#define WALL_THRESHOLD 0.5
#define OBSTACLE_THRESHOLD 0.8
#define PI 3.14
#define MAX 999
#define NORMAL_SPEED 0.5
#define SLOW_WALK 0.1

typedef enum{ GOAL_SEEK ,WALL_FOLLOW,AT_GOAL}STATE;
float g_speed = SLOW_WALK;
bool g_obstacleDetected;
float minimum,turn,range;
double robotAngle;
typedef struct
{
	float x;
	float y;
}Points;
Points g_startPos ={-8.0,-2.0};
Points g_endPos = {4.5 , 9.0};
Points g_robotPos;
Publisher vel;
Twist pos;
float islop, fslop;
/*split Merge declarations*/
Publisher linesPub,marker,markerarr;
visualization_msgs::Marker line_list;

/*funtion declarations*/
float goalDistance();
float goalAngle();
void stopRobot();
float getGoalSeekRotation();
void bug2_algorithm(Rate loop_rate);
void setVelocoty(float fwdVel,float rotVel);
void basePoseSubCallback(const Odometry msg);
void LaserSubCallback(const LaserScan scan);
void LaserSubCallback(const LaserScan scan);
vector< vector<Points> > splitMerge(Points point[],int start,int size);
double shortestDistance(Points p,Points p1,Points p2);
