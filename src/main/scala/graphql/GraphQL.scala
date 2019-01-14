package graphql

import com.google.inject.Inject
import graphql.schemas.PostSchema
import sangria.schema.{ObjectType, fields}

class GraphQL @Inject()(postSchema: PostSchema) {
  val Schema = sangria.schema.Schema(
    query = ObjectType("Query",
      fields(
        postSchema.Queries: _*
      )
    ),

    mutation = Some(
      ObjectType("Mutation",
        fields(
          postSchema.Mutations: _*
        )
      )
    )
  )
}
