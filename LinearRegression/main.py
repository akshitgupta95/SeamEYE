import matplotlib.pyplot as plt
import numpy as np
from sklearn import linear_model,preprocessing,metrics
from sklearn.model_selection import train_test_split
from scipy import interpolate
from math import sqrt

# index values of acceleration and gyroscope
ax = 1
ay = 2
az = 3
gx = 4
gy = 5
gz = 6

# load labels
labels = np.loadtxt('labels.csv', delimiter=',', unpack=True, skiprows=1)
print('labels')
print(labels)
print('features')
featureGY = []
featureAZ = []
featureAX = []

# interpolation
def cubicInterpolation(rawData):
    for i in range(1,4):
        values= np.where(rawData[i,] <= -15.5)
        timeSeries= rawData[0,values]
        timeSeries=np.reshape(timeSeries,-1)
        values= np.reshape(values,-1)
        newTime=np.delete(rawData[0,], values[1:values.size-1])
        newAx=np.delete(rawData[i,], values[1:values.size-1])
        f = interpolate.interp1d(newTime, newAx,kind='cubic')
        interpolatedFunction = f(rawData[0,])
        rawData[i,]=interpolatedFunction
    return rawData

# load multiple csv files here and make input features for regression model
for i in range(1,33):
    try:
        rawData = np.loadtxt('bowl' + str(i) + '.csv', delimiter=',', unpack=True, skiprows=1)
    # modify time indexes
        rawData= cubicInterpolation(rawData)
        rawData[0,] = rawData[0,] - rawData[0, 0]
        peak_gy = np.argmax(rawData[gy, ])
        featureGY.append(rawData[gy, peak_gy])
        peak_az = np.argmin(rawData[az, ])
        featureAZ.append(rawData[az, peak_az])
    # get AX max within one second after peak GY occurs

        AxToConsider=np.array((rawData[ax,])[peak_gy-40:peak_gy+40])
        featureAX.append(np.amin(AxToConsider))

    except:
        print "unable to read file"+str(i)


print(featureGY)
print(featureAZ)
print(featureAX)


# create Dataset
dataset=np.column_stack((np.array(featureGY),np.array(featureAX)))
# dataset=featureGY
# scale the features
# minmax scaling
# scaler = preprocessing.MinMaxScaler()
# scaled_df = scaler.fit_transform(dataset)
# print scaled_df
# Zero mean unit variance scaler
scaledDataset=preprocessing.scale(dataset)
print(scaledDataset)
X_train, X_test, y_train, y_test = train_test_split(scaledDataset, labels, test_size=0.33, random_state=42)
print X_train
print y_train
# train regression model
regressor = linear_model.LinearRegression()
regressor.fit(X_train, y_train)
print 'coefficients'
print regressor.coef_

# get output for test data
print regressor.predict(X_test)
print y_test

print sqrt(metrics.mean_squared_error(y_test, regressor.predict(X_test)))

# plt.scatter(X_test, y_test,  color='black')
# plt.plot(X_test, regressor.predict(X_test), color='blue', linewidth=3)
#
# plt.xticks(())
# plt.yticks(())
#
# plt.show()




# helpers for graph plotting
x = np.loadtxt('bowl' + str(9) + '.csv', delimiter=',', unpack=True, skiprows=1)
#
modified_time = x[0,] - x[0, 0]
x= cubicInterpolation(x)

#
#
# # print("time of occurence"+str(modified_time[peak_gy]))
#  plt.plot(modified_time, rawData[5,], label="Gy")
# fig, axs = plt.subplots(4)
# axs[0].plot(modified_time, x[5,],label="Gy")
# axs[1].plot(modified_time, x[3,],label="Az")
# axs[2].plot(modified_time, x[ay,],label="Ay")
# axs[3].plot(modified_time, x[ax,],label="ax")
# # axs[2].plot(modified_time, x[1,],label="Ax")
# # plt.plot(modified_time,x[5,], label="Gy")
# #
plt.plot(modified_time,x[ax,], label="Ax")
plt.xlabel('time')
plt.ylabel('Ax')
plt.title('Acceleration vs time')
plt.legend()
plt.show()


