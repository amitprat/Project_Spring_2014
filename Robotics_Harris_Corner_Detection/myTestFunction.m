function myTestFunction(A);
	clc;
	%parameters
	k_h = [-1 -1 -1; -1 8 -1; -1 -1 -1];
	sigma = 1;
	
	I = im2double(imread(A));
	%apply image filter
	Hsl = myImageFilter(I, k_h);
	figure('name','Convolution : Image After Applying myImageFilter');
	imshow(Hsl);
	
	%apply Edge filter
	[Im,Io,Ix,Iy] = myEdgeFilter(A,sigma); %sigma 1
	figure('name','Edge : Image After Applying myEdgeFilter');
	imshow(Im);
	
	%apply cornerness function
	H = myHarrisCorner(Ix,Iy,200);
	figure('name','Corner : myHarrisCorner Final plotted image');
	imshow(I);
	hold on;
	plot(H(:,1), H(:,2), 'r*');
end
