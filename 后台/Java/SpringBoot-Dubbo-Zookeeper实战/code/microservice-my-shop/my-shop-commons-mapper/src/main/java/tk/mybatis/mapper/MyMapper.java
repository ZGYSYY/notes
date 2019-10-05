package tk.mybatis.mapper;

import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.MySqlMapper;

/**
 * @author ZGY
 * @param <T>
 */
public interface MyMapper<T> extends Mapper, MySqlMapper<T> {
}
