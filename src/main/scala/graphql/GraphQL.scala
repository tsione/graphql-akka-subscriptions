package graphql

import com.google.inject.Inject
import graphql.schemas.PostSchema
import sangria.schema.{ObjectType, Schema, fields}

class GraphQL @Inject()(postSchema: PostSchema) {
  val schema: Schema[Unit, Unit] = sangria.schema.Schema(
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
