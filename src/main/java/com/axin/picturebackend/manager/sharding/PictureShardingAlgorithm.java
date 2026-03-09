//package com.axin.picturebackend.manager.sharding;
//
//import com.axin.picturebackend.exception.BusinessException;
//import com.axin.picturebackend.exception.ErrorCode;
//import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
//import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
//import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Properties;
//
//public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {
//
//	@Override
//	public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
//		Long spaceId = preciseShardingValue.getValue();
//		String logicTableName = preciseShardingValue.getLogicTableName();
//		if (spaceId == null) {
//			// 公共图库
//			return logicTableName;
//		}
//		// 私有空间或者团队空间
//		String realTableName = logicTableName + "_" + spaceId;
//		if (availableTargetNames.contains(realTableName)) {
//			return realTableName;
//		} else {
//			throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间不存在");
//		}
//	}
//
//	@Override
//	public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
//		return new ArrayList<>();
//	}
//
//	@Override
//	public Properties getProps() {
//		return null;
//	}
//
//	@Override
//	public void init(Properties properties) {
//
//	}
//}

