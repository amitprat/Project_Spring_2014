function I = myImageFilter(A, k);
if(size(size(A)) ~= 2) %apply check for color images
    A=rgb2gray(A);
end
[x1 y1] = size(A);
[k1 k2] = size(k);
center = floor((size(k)+1)/2);
left = center(2) - 1;
right = k2 - center(2);
top = center(1) - 1;
bottom = k1 - center(1);
temp = zeros(x1 + top + bottom, y1 + left + right);
%boundry condition check
for i = 1 + top : x1 + top
    for j = 1 + left : y1 + left
        temp(i,j) = A(i - top, j - left);
    end
end
I = zeros(x1 , y1);
for i = 1 : x1
    for j = 1 : y1
        for l = 1 : k1
            for m = 1 : k2
                q = i - 1;
                w = j - 1;
                I(i, j) = I(i, j) + temp(l + q, m + w) * k(l ,m);
            end
        end
    end
end