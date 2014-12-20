#include	"gprot.h"
#include	<lab2/Single_Line.h>
#include	<lab2/Lines.h>
using namespace lab2;
int main(int argc,char *argv[])
{
	ros::init(argc,argv,"splitMerge");
	NodeHandle n;
	Twist pos;
	linesPub = n.advertise<lab2::Lines>("lines",1);
	line_list.header.frame_id = "/base_link";
	line_list.header.stamp = ros::Time::now();
	line_list.ns = "mypoints";
	line_list.action = visualization_msgs::Marker::ADD;
	line_list.pose.orientation.w = 1.0;
	line_list.id = 2;
	line_list.type = visualization_msgs::Marker::LINE_LIST;
	// Line list is red
    	line_list.color.r = 1.0;
	line_list.color.a = 1.0;
	Rate loop_rate(10);
	Subscriber sub1 = n.subscribe("base_scan",1,LaserSubCallback);
	spin();
	return 0;
}
void LaserSubCallback(const LaserScan scan)
{
	int i=0,j=0,size = scan.ranges.size();
	double d, max = 0,theta;
	float l = 0;
	Lines line1;
	Single_Line line;
	Points point[361] = { {0,0} };
	vector< vector<Points> > lines;

	for(i=0;i<=360;i +=1,l +=0.5)
	{
		d = scan.ranges[i];
		if(d >= 3) continue;
		theta = l - 90 ;
		point[j].x = d*cos( (theta*PI)/180.0);
		point[j].y = d*sin( (theta*PI)/180.0); 
		j++;
	}
	lines = splitMerge(point,0,j-1);
	
	line1.num_lines = lines.size();
	for(int k=0;k<lines.size();k++)
	{
		double a,b,c;
		vector<Points>::iterator beg = lines[k].begin();
		vector<Points>::reverse_iterator end = lines[k].rbegin();
		Points p1 = *beg;
		Points p2 = *end;
		line.x1 = p1.x;
		line.y1 = p1.y;
		line.x2 = p2.x;		
		line.y2 = p2.y;
		line1.line.push_back(line);
		
	}
	linesPub.publish(line1);
}
double shortestDistance(Points p,Points p1,Points p2)
{
	double m = (p2.y - p1.y)/(p2.x - p1.x);
	return sqrt( pow( (p.x-m) , 2) + pow( (p.y-m) , 2) );
}
vector< vector<Points> > splitMerge(Points point[],int start,int end)
{
	double dmax = 0,d,epsilon = 1;
	int index = 0;
	vector< vector<Points> > lines,pointset1,pointset2;
	vector<Points> pointset;
	for(int i=start+1;i<end;i++)
	{
		d = shortestDistance(point[i],point[start],point[end]);
		if(d > dmax)
		{
			index = i;
			dmax = d;
		}
	}
	if(dmax > epsilon)
	{
		pointset1 = splitMerge(point,start,index);
		pointset2 = splitMerge(point,index,end);
		lines.insert(lines.end(),pointset1.begin(),pointset1.end());
		lines.insert(lines.end(),pointset2.begin(),pointset2.end());
	}
	else
	{
		pointset.push_back(point[start]);
		pointset.push_back(point[end]);
		lines.push_back(pointset);
	}
	return lines;
}
