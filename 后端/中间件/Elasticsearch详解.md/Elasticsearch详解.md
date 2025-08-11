<center><h1><b>Elasticsearch详解.md</b></h1></center>

# 目录

[TOC]

# 索引类

## 新增索引

示例如下：

```tex
PUT /user_details
{
  "settings": {
    "number_of_shards": 10
  }, 
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "user_id": {"type": "long"},
      "user_name": {"type": "keyword"},
      "user_remark": {"type": "text", "analyzer": "ik_max_word"}
    }
  }
}
```

属性说明：

| 属性                        | 必填 | 说明                                                         |
| --------------------------- | ---- | :----------------------------------------------------------- |
| settings.number_of_shards   | F    | 数据分片数量，将数据保存在几个分片中                         |
| settings.number_of_replicas | F    | 数据备份数量，将数据备份到其他几个节点中，如果是单节点，建议不设置或设置为 0，因为大于 0 不会生效 |
| mappings.dynamic            | F    | 动态映射，有 true（默认）、false、strict 三个值，如果新增文档中的字段在创建索引时没有被定义，会自动创建映射字段，生产环境建议设置为 strict，防止脏数据 |
| mappings.properties         | T    | 字段定义                                                     |

常用字段类型：

| 类型                      | 说明                       | 典型用途                                                     |
| ------------------------- | -------------------------- | ------------------------------------------------------------ |
| text                      | 分词文本，支持全文搜索     | 文章内容、评论文本                                           |
| keyword                   | 不分词字符串，支持精确匹配 | 用户ID、状态码、标签                                         |
| integer/long/float/double | 数字类型                   | 年龄、数量、时间戳                                           |
| date                      | 日期时间                   | 事件时间、注册日期                                           |
| boolean                   | 布尔类型                   | true/false                                                   |
| geo_point                 | 地理坐标                   | 地图应用                                                     |
| nested                    | 嵌套对象                   | 复杂结构数组（评论列表、订单明细），查询时需特殊nested查询方式 |
| object                    | 对象类型                   | 简单json对象，默认扁平化，不支持嵌套查询                     |

## 删除索引

示例如下：

```tex
DELETE /user_details
```

## 查询索引

查看索引全部信息，示例如下：

```tex
GET /user_details
```

查看索引设置，示例如下：

```
GET /user_details/_settings
```

查看索引映射，示例如下：

```tex
GET /user_details/_mapping
```

## 更新索引

在 Elasticsearch 里，**字段的类型一旦创建就不能直接修改**，因为 Mapping 是不可变的（字段类型会影响底层 Lucene 的存储结构）。

如果你发现字段类型定义错了，比如把 `price` 定成了 `text` 而不是 `double`，只能通过**新建索引 + 重建数据**来修正。

假设原索引叫 `products`，其中 `price` 是 `text` 类型，现在要改成 `double`。修复字段类型的常用流程如下：

1. 创建一个新索引，修正 mapping

    ```tex
    PUT /products_v2
    {
      "mappings": {
        "properties": {
          "name":  { "type": "text" },
          "price": { "type": "double" },
          "tags":  { "type": "keyword" }
        }
      }
    }
    ```

2. 将老索引数据复制到新索引

    ```tex
    POST /_reindex
    {
      "source": { "index": "products" },
      "dest":   { "index": "products_v2" }
    }
    ```

3. 删除旧索引并改名（可选）

    ```tex
    DELETE /products
    ```

4. 用别名让新索引接管

    ```
    POST /_aliases
    {
      "actions": [
        { "add": { "index": "products_v2", "alias": "products" } }
      ]
    }
    ```

# 文档类

## 新增文档

普通新增，示例如下：

```tex
PUT /user_details/_doc/1
{
  "user_id": 1,
  "user_name": "zgy",
  "user_remark": "不要迷恋哥，哥只是个传说！"
}
```

批量新增方式一，示例如下：

```tex
POST _bulk
{"index": { "_index": "user_details", "_id": "3" }}
{"user_id": 3,"user_name": "zyd","user_remark": "王老先生有块地！"}
{"index": { "_index": "user_details", "_id": "4" }}
{"user_id": 4,"user_name": "zn","user_remark": "我是迪迦奥特曼~~~"}
```

批量新增方式二，示例如下：

```tex
POST /user_details/_bulk
{"index": {"_id": "5" }}
{"user_id": 5,"user_name": "zfy","user_remark": "买家具找我！！！"}
{"index": {"_id": "6" }}
{"user_id": 6,"user_name": "wdm","user_remark": "出国旅游找我！！！"}
```

## 更新文档

## 删除文档

## 查询文档

### 查询结构基础

结构如下：

```tex
GET /index/_search
{
  "query": {
    // 查询类型
  },
  "from": 0,
  "size": 10,
  "sort": [],
  "aggs": {},
  "highlight": {}
}
```

- `query`：核心查询体，决定返回哪些文档。

- `from` + `size`：分页，默认返回前10条。

- `sort`：排序规则。

- `aggs`：聚合统计。

- `highlight`：高亮显示。

#### 查询类型

**基础查询**如下：

| 查询类型                | 说明                                     | 示例简要说明                                                 |
| ----------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| **match_all**           | 匹配所有文档，无任何过滤，通常做全表扫描 | `{ "match_all": {} }`                                        |
| **match**               | 对文本字段进行分词查询                   | `{ "match": { "field": "文本内容" } }`                       |
| **multi_match**         | 对多个字段进行 match 查询                | `{ "multi_match": { "query": "foo", "fields": ["title", "content"] } }` |
| **term**                | 精确值匹配，不分词                       | `{ "term": { "field.keyword": "exact" } }`                   |
| **terms**               | 多值精确匹配                             | `{ "terms": { "field.keyword": ["val1", "val2"] } }`         |
| **range**               | 范围查询（数字、日期、字符串）           | `{ "range": { "age": { "gte": 10, "lt": 20 } } }`            |
| **exists**              | 判断字段是否存在                         | `{ "exists": { "field": "field_name" } }`                    |
| **prefix**              | 前缀匹配                                 | `{ "prefix": { "field.keyword": "pre" } }`                   |
| **wildcard**            | 通配符查询，支持 * 和 ?                  | `{ "wildcard": { "field.keyword": "foo*" } }`                |
| **fuzzy**               | 模糊匹配，允许编辑距离                   | `{ "fuzzy": { "field": { "value": "roam", "fuzziness": 2 } } }` |
| **query_string**        | 支持 Lucene 查询语法，复杂全文查询       | `{ "query_string": { "query": "(foo OR bar) AND baz", "fields": ["title"] } }` |
| **simple_query_string** | 简化版 query_string，错误更容忍          | `{ "simple_query_string": { "query": "foo +bar -baz" } }`    |

**组合查询**如下：

| 查询类型           | 说明                                              | 示例简要说明                                                 |
| ------------------ | ------------------------------------------------- | ------------------------------------------------------------ |
| **bool**           | 布尔组合查询，支持 must, should, filter, must_not | `{ "bool": { "must": [...], "filter": [...], "should": [...], "must_not": [...] } }` |
| **constant_score** | 用过滤器查询且不计算相关度得分                    | `{ "constant_score": { "filter": { "term": { "status": "active" } } } }` |
| **dis_max**        | 多查询取最大得分                                  | `{ "dis_max": { "queries": [ {...}, {...} ], "tie_breaker": 0.7 } }` |
| **function_score** | 可以用函数对文档评分做动态调整                    | `{ "function_score": { "query": {...}, "functions": [ {...} ] } }` |

**嵌套与关联查询**如下：

| 查询类型       | 说明                                   | 示例简要说明                                                 |
| -------------- | -------------------------------------- | ------------------------------------------------------------ |
| **nested**     | 查询嵌套类型字段（数组对象）           | `{ "nested": { "path": "comments", "query": {...} } }`       |
| **has_child**  | 查询父子关系中有子文档满足条件的父文档 | `{ "has_child": { "type": "comment", "query": {...} } }`     |
| **has_parent** | 查询子文档满足条件的父文档             | `{ "has_parent": { "parent_type": "post", "query": {...} } }` |
| **parent_id**  | 查询指定父ID的子文档                   | `{ "parent_id": { "type": "comment", "id": "123" } }`        |

**地理位置查询**如下：

| 查询类型             | 说明                                 | 示例简要说明                                                 |
| -------------------- | ------------------------------------ | ------------------------------------------------------------ |
| **geo_distance**     | 查询距离指定经纬度的范围内文档       | `{ "geo_distance": { "distance": "12km", "location": { "lat": 40, "lon": -70 } } }` |
| **geo_bounding_box** | 查询指定矩形范围内地理点             | `{ "geo_bounding_box": { "location": { "top_left": {...}, "bottom_right": {...} } } }` |
| **geo_polygon**      | 查询指定多边形内的地理点             | `{ "geo_polygon": { "location": { "points": [ {...}, {...} ] } } }` |
| **geo_shape**        | 查询复杂地理形状（支持多种几何图形） | `{ "geo_shape": { "location": { "shape": {...}, "relation": "within" } } }` |

**特殊与辅助查询**如下：

| 查询类型           | 说明                             | 示例简要说明                                                 |
| ------------------ | -------------------------------- | ------------------------------------------------------------ |
| **ids**            | 根据文档ID列表查询               | `{ "ids": { "values": ["1", "2", "3"] } }`                   |
| **more_like_this** | 查找与给定文档类似的文档         | `{ "more_like_this": { "fields": ["title"], "like": "文本内容" } }` |
| **script**         | 用脚本实现自定义复杂查询         | `{ "script": { "script": "doc['field'].value > 10" } }`      |
| **percolate**      | 查询匹配预注册的查询（高级用法） | `{ "percolate": { "field": "query", "document": {...} } }`   |

**搜索建议（Suggesters）**如下：

| 查询类型                 | 说明         | 示例简要说明                                                 |
| ------------------------ | ------------ | ------------------------------------------------------------ |
| **term suggester**       | 单词纠错建议 | `{ "suggest": { "text": "quikc", "simple": { "term": { "field": "content" } } } }` |
| **phrase suggester**     | 短语纠错建议 | `{ "suggest": { "text": "the quikc brown fox", "phrase": { "field": "content" } } }` |
| **completion suggester** | 自动补全建议 | `{ "suggest": { "song-suggest": { "prefix": "nirv", "completion": { "field": "suggest" } } } }` |

**全文检索特殊查询**如下：

| 查询类型                | 说明                                      | 示例简要说明                                              |
| ----------------------- | ----------------------------------------- | --------------------------------------------------------- |
| **simple_query_string** | 简化版 query_string，容错性更好，语法简单 | `{ "simple_query_string": { "query": "foo +bar -baz" } }` |

#### 查询结果解释

| 字段名           | 类型         | 说明                                                         |
| :--------------- | ------------ | ------------------------------------------------------------ |
| `took`           | 数字（毫秒） | 查询耗时，单位是毫秒，比如 `4` 表示查询用了 4 毫秒。         |
| `timed_out`      | 布尔值       | 查询是否超时，`false` 表示正常完成，`true` 表示查询超时。    |
| `_shards`        | 对象         | 查询涉及的分片统计信息。                                     |
| ├─ `total`       | 数字         | 查询的分片总数，比如 `10` 个分片。                           |
| ├─ `successful`  | 数字         | 成功响应的分片数量。                                         |
| ├─ `skipped`     | 数字         | 被跳过的分片数量（例如优化或缓存）。                         |
| └─ `failed`      | 数字         | 失败的分片数量。                                             |
| `hits`           | 对象         | 查询命中的结果。                                             |
| ├─ `total`       | 对象         | 命中总数信息。                                               |
| │  ├─ `value`    | 数字         | 命中文档的数量，比如 `2` 条。                                |
| │  └─ `relation` | 字符串       | 命中数的关系，`eq` 表示精确计数，`gte` 表示大于等于估算。    |
| ├─ `max_score`   | 数字或空值   | 命中结果中最高的相关度分数，若无评分则为 `null`。            |
| └─ `hits`        | 数组         | 命中的具体文档数组，每个元素包含文档详情（`_index`、`_id`、`_source` 等）。 |

### 简单查询

单文档查询，示例如下：

```tex
GET /user_details/_doc/1
```

查看索引所有数据，示例如下：

```
GET /user_details/_search
{
  "query": {
    "match_all": {}
  }
}
```

关键字查询，示例如下：

```tex
GET /user_details/_search
{
  "query": {
    "match": {
      "user_remark": "找我"
    }
  }
}
```

查询并排序，示例如下：

```tex
GET /user_details/_search
{
  "sort": [
    {
      "user_id": {
        "order": "asc"
      }
    }
  ], 
  "query": {
    "match": {
      "user_remark": "找我"
    }
  }
}
```

查询结果高亮，示例如下：

```tex
GET /user_details/_search
{
  "query": {
    "match": {
      "user_remark": "找我"
    }
  },
  "highlight": {
    "fields": {
      "user_remark": {}
    }
  }
}
```

### 组合查询

### 聚合查询

### 分页查询