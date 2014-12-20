function [Im Io Ix Iy] = myEdgeFilter(img, sigma);
IMG=im2double(imread(img));

%color image check
if(size(size(IMG)) ~= 2)
    IMG = rgb2gray(IMG);
end

%calculate gaussian filter
h = sigma * 3;
%h = fspecial('gaussian',3,sigma);
[xx, yy] = meshgrid(-h:h, -h:h);
G = exp(-(xx .^ 2 + yy .^ 2) / (2 * sigma ^ 2));

%image smoothing
IMG=myImageFilter(double(IMG),double(h));

%soble filter
K_V = [ -1 0 1; -2 0 2; -1 0 1];
K_H = [ 1 2 1; 0 0 0; -1 -2 -1];
Ix=myImageFilter(double(IMG),double(K_H));
Iy=myImageFilter(double(IMG),double(K_V));     

[r1 s1] = size(IMG); %size calculation of matrix
Io=atan(Iy/Ix);

%magnitude calculation
for i=1:r1
   for j=1:s1
		Im(i,j)=(Ix(i,j).^2+Iy(i,j).^2).^0.5;
   end
end

%apply edge thinning
[r s] = size(Im); 
for x=2:r-1
   for y=2:s-1
       if( Im(x,y) < Im(x-1,y) || Im(x,y) < Im(x,y-1) || Im(x,y) < Im(x+1,y) || Im(x,y) < Im(x,y+1) )
            Im(x,y) = 0;
       end
    end
end
end