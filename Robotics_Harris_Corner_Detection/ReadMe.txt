/*************************************** Read Me File ******************************************/

1) I am including one extra file (myTestFunction.m) that i used as test file to display images at all phases.
 It requires only one parameter (say 'img01.jpg') and it will generate 4 figures ( 1- convolution, 1- edge detection, 2 - corner detection).
 In this testing file : sigma - 1 , threshold - 200
 
2) Each function can be tested for separate output.
3) For harris corner function, superimposed image can be checked as below:
	H = myHarrisCorner()
	imshow(I);
	hold on;
	plot(H(:,2), H(:,1), 'r*');