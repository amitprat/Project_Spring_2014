% paramters
function H = myHarrisCorner(Ix, Iy, threshold);

% paramters
sigma = 1;
k = 0.04;
corners = 0;

%display image for plot
figure('name','Corner : myHarrisCorner Intermdeiate plotted image')
if( exist('I') == 0)
	Im=(Ix.^2+Iy.^2).^0.5;
	imshow(Ix);
	hold on
end

%derivatives at each pixel
Ix2 = Ix .^ 2;
Iy2 = Iy .^ 2;
Ixy = Ix .* Iy;

%calculate gaussian filter
h = sigma* 3;
[xx, yy] = meshgrid(-h:h, -h:h);
G = exp(-(xx .^ 2 + yy .^ 2) / (2 * sigma ^ 2));

%sum of derivatives at each pixel
Sx2 = myImageFilter(Ix2, G);
Sy2 = myImageFilter(Iy2, G);
Sxy = myImageFilter(Ixy, G);
%find size
[numOfRows numOfColumns] = size(Ixy);

%initialize image
H = zeros(numOfRows+numOfColumns,2);
%for each pixel , apply cornerness function and plot it
for x=1:numOfRows,
   for y=1:numOfColumns
       M = [Sx2(x, y) Sxy(x, y); Sxy(x, y) Sy2(x, y)]; %Matrix calculation
       R = det(M) - k * (trace(M) ^ 2); %cornerness function
	   
       %Threshold on value of R
       if (R > threshold)
          H(x+y,2) = x; 
		  H(x+y,1) = y; 
		  plot(y,x, 'r*');
		  corners = corners +1;
       end
   end
end
disp(corners);
end